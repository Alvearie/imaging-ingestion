/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package manager

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"sync"
	"time"

	"github.com/pkg/errors"

	jsmcloudevents "github.com/cloudevents/sdk-go/protocol/nats_jetstream/v2"
	cloudevents "github.com/cloudevents/sdk-go/v2"
	"github.com/nats-io/nats.go"
	"github.com/nats-io/stan.go"
	"go.uber.org/zap"
)

const (
	// maxReconnectRequests defines a maximum number of outstanding re-connect requests
	maxReconnectRequests = 10
)

var (
	// natsConnectionRetryInterval defines delay in seconds for the next attempt to reconnect to NATS JetStream server
	natsConnectionRetryInterval = 1 * time.Second
)

type BridgeRole string

const (
	BridgeRoleHub  BridgeRole = "hub"
	BridgeRoleEdge BridgeRole = "edge"
)

type NatsManagerArgs struct {
	Logger *zap.Logger

	NatsURL            string
	Role               BridgeRole
	EdgeMailboxID      string
	EdgeLocationConfig string
	Sink               string
}

type NatsManager struct {
	logger        *zap.Logger
	role          BridgeRole
	receiver      *Receiver
	sender        *Sender
	edgeMailboxID string
	edgeLocations map[string]string
	sink          string

	connect            chan struct{}
	natsURL            string
	natsConnMux        sync.Mutex
	natsConn           *nats.Conn
	natsConnInProgress bool
}

type Receiver struct {
	logger       *zap.Logger
	client       cloudevents.Client
	eventManager *NatsManager
}

type Sender struct {
	logger       *zap.Logger
	client       cloudevents.Client
	ctx          context.Context
	eventManager *NatsManager
}

func NewNatsManager(args NatsManagerArgs) (*NatsManager, error) {
	if args.Logger == nil {
		args.Logger = zap.NewNop()
	}

	m := &NatsManager{
		logger: args.Logger,
		role:   args.Role,

		connect:       make(chan struct{}, maxReconnectRequests),
		natsURL:       args.NatsURL,
		edgeMailboxID: args.EdgeMailboxID,
		sink:          args.Sink,
	}

	client, err := cloudevents.NewDefaultClient()
	if err != nil {
		args.Logger.Fatal("Failed to create cloudevents receiver client", zap.Error(err))
	}

	r := &Receiver{
		logger:       args.Logger,
		client:       client,
		eventManager: m,
	}
	m.receiver = r

	s := &Sender{
		logger:       args.Logger,
		client:       client,
		eventManager: m,
		ctx:          cloudevents.ContextWithTarget(context.Background(), m.sink),
	}
	m.sender = s

	if args.Role == BridgeRoleHub {
		// Read edge locations
		loc, err := loadEdgeLocations(args)
		if err != nil {
			args.Logger.Fatal("Failed to load edge locations", zap.Error(err))
		}
		m.edgeLocations = loc
	}

	return m, nil
}

func loadEdgeLocations(args NatsManagerArgs) (map[string]string, error) {
	locations := make(map[string]string)

	bytes, err := ioutil.ReadFile(args.EdgeLocationConfig)
	if err != nil {
		return locations, fmt.Errorf("Error reading EDGE_LOCATION_CONFIG", err)
	}

	err = json.Unmarshal(bytes, &locations)
	if err != nil {
		return locations, fmt.Errorf("Error unmarshalling EDGE_LOCATION_CONFIG", err)
	}

	return locations, nil
}

func (r *Receiver) Receive(ctx context.Context, event cloudevents.Event) error {
	r.logger.Info("Received event")

	r.eventManager.natsConnMux.Lock()
	currentNatssConn := r.eventManager.natsConn
	r.eventManager.natsConnMux.Unlock()
	if currentNatssConn == nil {
		r.logger.Error("no Connection to NATS JetStream")
		return errors.New("no Connection to NATS JetStream")
	}

	subj := r.eventManager.getPublishSubject(event)
	sender, err := jsmcloudevents.NewSenderFromConn(currentNatssConn, GetStreamName(), subj, nil)
	if err != nil {
		r.logger.Error("could not create nats jetstream sender", zap.Error(err))
		return errors.Wrap(err, "could not create nats jetstream sender")
	}

	bytes, err := json.Marshal(event)
	if err != nil {
		r.logger.Error("could not marshal event to bytes", zap.Error(err))
		return errors.Wrap(err, "could not marshal event to bytes")
	}

	natsMsg := &nats.Msg{
		Data: bytes,
	}

	if err := sender.Send(ctx, jsmcloudevents.NewMessage(natsMsg)); err != nil {
		errMsg := "error during send"
		if err.Error() == stan.ErrConnectionClosed.Error() {
			errMsg += " - connection to NATSS has been lost, attempting to reconnect"
			r.eventManager.signalReconnect()
		}
		r.logger.Error(errMsg, zap.Error(err))
		return errors.Wrap(err, errMsg)
	}
	r.logger.Info("event published to " + subj)
	return nil
}

func (m *NatsManager) Subscribe(ctx context.Context) error {
	fn := func(msg *nats.Msg) {
		defer func() {
			if r := recover(); r != nil {
				m.logger.Warn("Panic happened while handling a message",
					zap.String("messages", string(msg.Data)),
					zap.Any("panic value", r),
				)
			}
		}()

		m.logger.Debug(string(msg.Data))

		event := cloudevents.NewEvent()
		err := json.Unmarshal(msg.Data, &event)
		if err != nil {
			m.logger.Error("failed to unmarshal event", zap.Error(err))
		}

		if result := m.sender.client.Send(m.sender.ctx, event); cloudevents.IsUndelivered(result) {
			m.logger.Error("failed to send event to sink")
			return
		}

		if err := msg.Ack(); err != nil {
			m.logger.Error("failed to acknowledge message", zap.Error(err))
			return
		}

		m.logger.Info("message sent to sink")
	}

	m.natsConnMux.Lock()
	currentNatssConn := m.natsConn
	m.natsConnMux.Unlock()

	if currentNatssConn == nil {
		return errors.New("no Connection to NATS JetStream")
	}

	jsm, err := currentNatssConn.JetStream(nil...)
	if jsm == nil || err != nil {
		return fmt.Errorf("get JetStream Context from Connection err, err:%s", err.Error())
	}

	subj := m.getSubscribeSubject()
	_, err = jsm.Subscribe(subj, fn)
	if err != nil {
		m.logger.Error("Create new NATS JetStream Subscription failed: ", zap.Error(err))
		if err.Error() == stan.ErrConnectionClosed.Error() {
			m.logger.Error("Connection to NATS JetStream has been lost, attempting to reconnect.")
			m.signalReconnect()
			return err
		}
		return err
	}
	m.logger.Info("Subscribing to " + subj)

	return nil
}

func (r *Receiver) Start(ctx context.Context) {
	r.logger.Info("Starting cloudevents receiver")
	if err := r.client.StartReceiver(ctx, r.Receive); err != nil {
		r.logger.Fatal("Error starting cloudevents receiver", zap.Error(err))
	}
}

func (m *NatsManager) Start(ctx context.Context) error {
	// Starting Connect to establish connection with NATS
	go m.Connect(ctx)
	// Trigger Connect to establish connection with NATS
	m.signalReconnect()

	go m.receiver.Start(ctx)

	// Wait for NATS Connection
	ticker := time.NewTicker(1 * time.Second)
	for range ticker.C {
		if m.IsConnected() {
			break
		}
		m.logger.Info("No connection to NATS JetStream, retrying ...")
	}

	err := m.Subscribe(ctx)
	if err != nil {
		m.logger.Fatal("Error subscribing to NATS stream", zap.Error(err))
	}

	return nil
}

func (m *NatsManager) Connect(ctx context.Context) {
	for {
		select {
		case <-m.connect:
			m.natsConnMux.Lock()
			currentConnProgress := m.natsConnInProgress
			m.natsConnMux.Unlock()
			if !currentConnProgress {
				// Case for lost connectivity, setting InProgress to true to prevent recursion
				m.natsConnMux.Lock()
				m.natsConnInProgress = true
				m.natsConnMux.Unlock()
				go m.connectWithRetry(ctx)
			}
		case <-ctx.Done():
			return
		}
	}
}

func (m *NatsManager) connectWithRetry(ctx context.Context) {
	// re-attempting evey 1 second until the connection is established.
	ticker := time.NewTicker(natsConnectionRetryInterval)
	defer ticker.Stop()
	for {
		nConn, err := m.JetStreamConnect(m.natsURL, m.logger.Sugar())
		if err == nil {
			// Locking here in order to reduce time in locked state.
			m.natsConnMux.Lock()
			m.natsConn = nConn
			m.natsConnInProgress = false
			m.natsConnMux.Unlock()
			return
		}
		m.logger.Sugar().Errorf("Connect() failed with error: %+v, retrying in %s", err, natsConnectionRetryInterval.String())
		select {
		case <-ticker.C:
			continue
		case <-ctx.Done():
			return
		}
	}
}

func (m *NatsManager) signalReconnect() {
	select {
	case m.connect <- struct{}{}:
		// Sent.
	default:
		// The Channel is already full, so a reconnection attempt will occur.
	}
}

func (m *NatsManager) IsConnected() bool {
	m.natsConnMux.Lock()
	currentNatssConn := m.natsConn
	m.natsConnMux.Unlock()

	return currentNatssConn != nil
}

func (m *NatsManager) getPublishSubject(event cloudevents.Event) string {
	if m.role == BridgeRoleEdge {
		return GetStreamName() + ".wheel.hub.mailbox"
	} else if m.role == BridgeRoleHub {
		e := make(map[string]string)

		m.logger.Debug(string(event.Data()))

		err := json.Unmarshal(event.Data(), &e)
		if err != nil {
			m.logger.Error("Error unmarshalling cloud event", zap.Error(err))
			return GetInboxPrefix()
		}

		if t, ok := e["target"]; ok {
			if mb, ok := m.edgeLocations[t]; ok {
				return GetStreamName() + ".wheel." + mb + ".mailbox"
			} else {
				m.logger.Error("Error getting target mailbox id from edge locations", zap.Error(err))
				return GetInboxPrefix()
			}
		} else {
			m.logger.Error("Error getting target edge location from cloud event", zap.Error(err))
			return GetInboxPrefix()
		}
	}

	return ""
}

func (m *NatsManager) getSubscribeSubject() string {
	if m.role == BridgeRoleEdge {
		return GetStreamName() + ".wheel." + m.edgeMailboxID + ".mailbox"
	} else if m.role == BridgeRoleHub {
		return GetStreamName() + ".wheel.hub.mailbox"
	}

	return ""
}

func (m *NatsManager) getStreamSubjects() []string {
	subj := []string{m.getSubscribeSubject()}
	if m.role == BridgeRoleEdge {
		subj = append(subj, m.getPublishSubject(cloudevents.NewEvent()))
	} else if m.role == BridgeRoleHub {
		for _, v := range m.edgeLocations {
			subj = append(subj, GetStreamName()+".wheel."+v+".mailbox")
		}
	}

	return subj
}
