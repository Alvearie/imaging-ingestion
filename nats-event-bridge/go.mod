module github.com/Alvearie/imaging-ingestion/nats-event-bridge

go 1.15

require (
	github.com/cloudevents/sdk-go/protocol/nats_jetstream/v2 v2.0.0-20210715165402-49fda7a51425
	github.com/cloudevents/sdk-go/v2 v2.4.1
	github.com/dgrijalva/jwt-go v3.2.0+incompatible
	github.com/kelseyhightower/envconfig v1.4.0
	github.com/nats-io/nats-streaming-server v0.23.2 // indirect
	github.com/nats-io/nats.go v1.13.1-0.20211018182449-f2416a8b1483
	github.com/nats-io/stan.go v0.10.2
	github.com/pkg/errors v0.9.1
	go.uber.org/zap v1.19.1
)

replace github.com/cloudevents/sdk-go/v2 => github.com/cloudevents/sdk-go/v2 v2.4.1-0.20210715165402-49fda7a51425
