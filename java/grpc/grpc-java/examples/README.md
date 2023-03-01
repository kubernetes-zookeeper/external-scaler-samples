KEDA gRPC Example
==============================================

You may want to read through the
[Quick Start](https://grpc.io/docs/languages/java/quickstart)
before trying out the examples.

## KEDA External Scaler Server

- [External scaler](src/main/java/io/grpc/examples/externalscaler)

### <a name="to-build-the-examples"></a> To build the [KEDA](https://keda.sh/docs/latest/concepts/external-scalers/) External Scaler Server example 

1. From grpc-java/examples directory:
```
$ ./gradlew clean docker
```

This creates the docker image `kuberneteszookeeper/external-scaler-server`.
The `kuberneteszookeeper/external-scaler-server` docker image should be accessible from the k8s clsuter.

To try the external scaler please run:

```
$ ./keda/install_keda.sh
$ cd helm
$ ./helm_install.sh
$ helm upgrade --install external-scaler-server ./external-scaler-server/ --namespace external-scaler-server --create-namespace --values ./external-scaler-server/values.yaml
```

That's it!<br>
The [external KEDA](https://keda.sh/docs/latest/scalers/external/) ScaledObject is now periodically polling the external-scaler-server (over gRPC protocol) for the name/value of the metric.
Based on the value (and the target size for this metric), KEDA is scaling the related nginx Deployment/StatefulSet.

You may run the following:

```
$ kubectl get pods -n external-scaler-server
$ kubectl -n external-scaler-server logs -f {external-scaler-server-deployment}
```

For more information, refer to KEDA
[keda](https://keda.sh/).
