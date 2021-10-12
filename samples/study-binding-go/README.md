# study-binding-go

Sample project to consume `StudyRevisionEvent`.

## Build

```
make docker-build
make docker-push
```

## Test Locally

```
# In one terminal window

go run main.go

# In another terminal window

curl -XPOST -v "http://localhost:8080" \
-H "Ce-Id: 1234" \
-H "Ce-Specversion: 1.0" \
-H "Ce-Type: StudyRevisionEvent" \
-H "Ce-Source: curl" \
-H "Content-Type: application/json" \
-d @test/event.json
```
