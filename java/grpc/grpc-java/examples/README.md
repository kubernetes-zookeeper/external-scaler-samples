KEDA gRPC Example
==============================================

You may want to read through the
[Quick Start](https://grpc.io/docs/languages/java/quickstart)
before trying out the examples.

## KEDA External Scaler Server

- [External scaler](src/main/java/io/grpc/examples/externalscaler)

### <a name="to-build-the-examples"></a> To build the KEDA External Scaler Server example 

1. From grpc-java/examples directory:
```
$ ./gradlew clean docker
```

This creates the docker image `kuberneteszookeeper/external-scaler-server`.
The `kuberneteszookeeper/external-scaler-server` docker image should be accessible from the k8s clsuter.

To try the external scaler first run:

```
$ ./keda/install_keda.sh
$ cd helm
$ ./helm_install.sh
$ helm upgrade --install external-scaler-server ./external-scaler-server/ --namespace external-scaler-server --create-namespace --values ./external-scaler-server/values.yaml
```

That's it!

For more information, refer to gRPC Java's [README](../README.md) and
[tutorial](https://grpc.io/docs/languages/java/basics).

### Maven

If you prefer to use Maven:
1. **[Install gRPC Java library SNAPSHOT locally, including code generation plugin](../COMPILING.md) (Only need this step for non-released versions, e.g. master HEAD).**

2. Run in this directory:
```
$ mvn verify
$ # Run the server
$ mvn exec:java -Dexec.mainClass=io.grpc.examples.externalscaler.ExternalScalerServer
$ # In another terminal run the client
$ mvn exec:java -Dexec.mainClass=io.grpc.examples.externalscaler.ExternalScalerClient
```

### Bazel

If you prefer to use Bazel:
```
$ bazel build :external-scaler-server :external-scaler-client
$ # Run the server
$ bazel-bin/external-scaler-server
$ # In another terminal run the client
$ bazel-bin/external-scaler-client
```

## Other examples

- [Android examples](android)

- Secure channel examples

  + [TLS examples](example-tls)

  + [ALTS examples](example-alts)

- [Google Authentication](example-gauth)

- [JWT-based Authentication](example-jwt-auth)

## Unit test examples

Examples for unit testing gRPC clients and servers are located in [examples/src/test](src/test).

In general, we DO NOT allow overriding the client stub and we DO NOT support mocking final methods
in gRPC-Java library. Users should be cautious that using tools like PowerMock or
[mockito-inline](https://search.maven.org/search?q=g:org.mockito%20a:mockito-inline) can easily
break this rule of thumb. We encourage users to leverage `InProcessTransport` as demonstrated in the
examples to write unit tests. `InProcessTransport` is light-weight and runs the server
and client in the same process without any socket/TCP connection.

Mocking the client stub provides a false sense of security when writing tests. Mocking stubs and responses
allows for tests that don't map to reality, causing the tests to pass, but the system-under-test to fail.
The gRPC client library is complicated, and accurately reproducing that complexity with mocks is very hard.
You will be better off and write less code by using `InProcessTransport` instead.

Example bugs not caught by mocked stub tests include:

* Calling the stub with a `null` message
* Not calling `close()`
* Sending invalid headers
* Ignoring deadlines
* Ignoring cancellation

For testing a gRPC client, create the client with a real stub
using an and test it against an with a mock/fake service implementation.

For testing a gRPC server, create the server as an InProcessServer,
and test it against a real client stub with an InProcessChannel.

The gRPC-java library also provides a JUnit rule, to do the graceful
shutdown boilerplate for you.

## Even more examples

A wide variety of third-party examples can be found [here](https://github.com/saturnism/grpc-java-by-example).
