# study-binding-quarkus

Sample project to consume `StudyRevisionEvent`.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/study-binding-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

## Related Guides

- Funqy Knative Events Binding ([guide](https://quarkus.io/guides/funqy-knative-events)): Knative Events Binding for Quarkus Funqy framework

## Provided Code

### Funqy Knative Events Binding example

This example contains a Quarkus Knative Funqy Function ready for Kubernetes or Openshift.

[Related guide section...](https://quarkus.io/guides/funqy-knative-events)

You'll need to read the guide to learn how to deploy and run this example within a full Knative Kubernetes or OpenShift
environment.

This example is incomplete until you specify your docker.io account name within `src/main/k8s/funqy-service.yaml`.

## Test Locally

```
# In one terminal window

./gradlew quarkusDev

# In another terminal window

curl -XPOST -v "http://localhost:8080" \
-H "Ce-Id: 1234" \
-H "Ce-Specversion: 1.0" \
-H "Ce-Type: StudyRevisionEvent" \
-H "Ce-Source: curl" \
-H "Content-Type: application/json" \
-d @src/test/resources/event.json
```
