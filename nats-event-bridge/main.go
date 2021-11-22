/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package main

import (
	"context"

	"github.com/Alvearie/imaging-ingestion/nats-event-bridge/manager"
	"github.com/kelseyhightower/envconfig"
	"go.uber.org/zap"
)

type envConfig struct {
	Role               manager.BridgeRole `envconfig:"BRIDGE_ROLE" required:"true"`
	NatsURL            string             `envconfig:"NATS_URL" required:"true"`
	EdgeMailboxID      string             `envconfig:"EDGE_MAILBOX_ID" required:"false"`
	EdgeLocationConfig string             `envconfig:"EDGE_LOCATION_CONFIG" required:"false"`
	Sink               string             `envconfig:"K_SINK" required:"true"`
}

var logger *zap.SugaredLogger

func init() {
	if l, err := zap.NewProduction(); err != nil {
		// We failed to create a fallback logger. Our fallback
		// unfortunately falls back to noop.
		logger = zap.NewNop().Sugar()
	} else {
		logger = l.Named("nats-event-bridge").Sugar()
	}
}

func main() {
	ctx := context.Background()

	var env envConfig
	if err := envconfig.Process("", &env); err != nil {
		logger.Fatalw("Failed to process env var", zap.Error(err))
	}

	if env.Role != manager.BridgeRoleEdge && env.Role != manager.BridgeRoleHub {
		logger.Fatal("Invalid BRIDGE_ROLE: " + env.Role)
	}

	if env.Role == manager.BridgeRoleEdge && env.EdgeMailboxID == "" {
		logger.Fatal("EDGE_MAILBOX_ID is not set for BRIDGE_ROLE: " + env.Role)
	}

	if env.Role == manager.BridgeRoleHub && env.EdgeLocationConfig == "" {
		logger.Fatal("EDGE_LOCATION_CONFIG is not set for BRIDGE_ROLE: " + env.Role)
	}

	logger.Info("Bridge Role: ", env.Role)

	args := manager.NatsManagerArgs{
		Logger:             logger.Desugar(),
		Role:               env.Role,
		NatsURL:            env.NatsURL,
		EdgeMailboxID:      env.EdgeMailboxID,
		EdgeLocationConfig: env.EdgeLocationConfig,
		Sink:               env.Sink,
	}

	manager, err := manager.NewNatsManager(args)
	if err != nil {
		logger.Fatalw("Failed to create NATS manager", zap.Error(err))
	}

	err = manager.Start(ctx)
	if err != nil {
		logger.Fatalw("Failed to start NATS manager", zap.Error(err))
	}
	logger.Info("NATS Events Bridge started")
	<-ctx.Done()
}
