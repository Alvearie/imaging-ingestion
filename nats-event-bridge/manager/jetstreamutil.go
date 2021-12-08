/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package manager

import (
	"crypto/tls"
	"os"
	"strings"

	"github.com/nats-io/nats.go"

	"go.uber.org/zap"
)

const (
	//  DefaultStreamName is the default name of StreamConfig for JetStream
	DefaultStreamName = "events"

	// MaxPending is the maximum outstanding async publishes that can be inflight at one time.
	MaxPending = 256
)

// JetStreamConnect creates a new NATS JetStream connection
func (m *NatsManager) JetStreamConnect(jetStreamUrl string, logger *zap.SugaredLogger) (*nats.Conn, error) {
	logger.Infof("JetStreamConnect(): jetStreamUrl: %v", jetStreamUrl)

	opts := make([]nats.Option, 0)

	tlsEnabled := GetEnv("NATS_TLS_ENABLED", "false")
	if strings.ToLower(tlsEnabled) == "true" {
		logger.Info("Enabling TLS connection")
		insecureSkipVerify := strings.ToLower(GetEnv("INSECURE_SKIP_VERIFY", "false")) == "true"

		opts = append(opts, nats.Secure(&tls.Config{
			InsecureSkipVerify: insecureSkipVerify,
		}))
	}

	token := GetEnv("NATS_AUTH_TOKEN", "")
	if token != "" {
		user, err := GetUser(token)
		if err != nil {
			logger.Errorf("Connect(): create new connection failed: %v", err)
			return nil, err
		}

		opts = append(opts, nats.UserInfo(user, token))
	}

	inboxPrefix := GetInboxPrefix()
	opts = append(opts, nats.CustomInboxPrefix(inboxPrefix))

	nc, err := nats.Connect(jetStreamUrl, opts...)
	if err != nil {
		logger.Errorf("Connect(): create new connection failed: %v", err)
		return nil, err
	}
	logger.Infof("Connect(): connection to NATS JetStream established!")

	// Create JetStream Context
	js, err := nc.JetStream(nats.PublishAsyncMaxPending(MaxPending))
	if err != nil {
		logger.Errorf("Connect(): create JetStream connection failed: %v", err)
		return nil, err
	}

	streamConfig := nats.StreamConfig{
		Name:      GetStreamName(),
		Subjects:  m.getStreamSubjects(),
		Retention: nats.WorkQueuePolicy,
	}

	_, err = getOrAddStream(js, &streamConfig)
	if err != nil {
		logger.Errorf("Connect(): getOrAddStream %#v failed: %v", streamConfig, err)
		return nil, err
	}
	return nc, nil
}

func getOrAddStream(js nats.JetStreamContext, cfg *nats.StreamConfig) (*nats.StreamInfo, error) {
	info, _ := js.StreamInfo(cfg.Name)
	if info != nil {
		return info, nil
	} else {
		return js.AddStream(cfg)
	}
}

// GetStreamName gets NATS stream name
func GetStreamName() string {
	return GetEnv("NATS_SUBJECT_ROOT", DefaultStreamName)
}

// GetEnv looks up env variable by key and defaults to fallback if env
// doesn't exists
func GetEnv(envKey string, fallback string) string {
	val, ok := os.LookupEnv(envKey)
	if !ok {
		return fallback
	}
	return val
}

func GetInboxPrefix() string {
	return GetStreamName() + "._INBOX"
}
