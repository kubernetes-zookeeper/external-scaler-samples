Auto Scaling of Kubernetes pods using KEDA External Scaler
==========================================================

You may want to read through the
[KEDA](https://keda.sh/)
before trying out the following example.

KEDA is a Kubernetes-based Event Driven Autoscaler. With KEDA, you can drive the scaling of any container in Kubernetes based on custom application metrics that are external to Kubernetes.<br>
KEDA is introducing a new Kubernetes Custom Resource Definition called [ScaledObject](https://keda.sh/docs/2.9/concepts/scaling-deployments/#scaledobject-spec).
KEDA ScaledObject is used to define how KEDA should scale your application and what the triggers are.<br>
The external ScaledObject can be configured to periodically poll your application (over gRPC protocol) to get the name/value of the custom application metrics that should control the number of pods (`replicas`) for a specific Kubernetes Deployment/StatefulSet.<br><br>
This simple example is showing how to easilly add the related gRPC endpoints to your application to support such custom Auto Scaling.<br>
The gRPC API endpoints that should be added to your application are specified by the [externalscaler.proto](external-scaler-grpc/src/main/proto/externalscaler.proto).<br>
The following example is building and running `external-scaler-server` that is serving as the gRPC server of your application.<br>
Based on the periodic response from the `external-scaler-server`, KEDA external [ScaledObject](external-scaler-grpc/helm/external-scaler-server/templates/keda_scaled_object_deployment.yaml) is scaling the number of `worker` pods (`replicas`).<br>


![KEDA-External-Scaler](https://github.com/kubernetes-zookeeper/external-scaler-samples/assets/112578195/3cf7672f-17c0-4df2-a439-c1d5b85cd1b0)


## KEDA External Scaler Server

- [External scaler](external-scaler-grpc/src/main/java/io/grpc/examples/externalscaler)

### <a name="to-build-the-examples"></a> To build the [KEDA](https://keda.sh/docs/latest/concepts/external-scalers/) External Scaler Server example 

1) From the top level `external-scaler-samples` directory:
   <br>
   Download gradle 7.4 package and extract it to the gradle folder.
```
wget -O gradle.zip https://services.gradle.org/distributions/gradle-7.4-bin.zip && unzip gradle.zip && /bin/mv gradle-7.4 gradle

./gradle/bin/gradle clean docker
```

This creates the docker images `kuberneteszookeeper/external-scaler-web` and `kuberneteszookeeper/external-scaler-server`.
The `kuberneteszookeeper/external-scaler-web` and `kuberneteszookeeper/external-scaler-server` docker image should be accessible from the k8s cluster.
<br><br>
2) To try the external scaler please run (from the top level `external-scaler-samples` directory):

```
./external-scaler-grpc/helm/install_helm.sh
```
```
./external-scaler-grpc/keda/install_keda.sh
```
```
helm upgrade --install external-scaler-server ./external-scaler-grpc/helm/external-scaler-server/ --namespace external-scaler-server --create-namespace --values ./external-scaler-grpc/helm/external-scaler-server/values.yaml --set registry=$AWS_ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com
```
The [external](https://keda.sh/docs/latest/scalers/external/) KEDA ScaledObject is now periodically polling the external-scaler-server (over gRPC protocol) for the name/value of the metric.
Based on the value (and the target size for this metric), KEDA is scaling the related worker Deployment/StatefulSet.
In this specific example the `metricName` is `"number_of_jobs"` and its `targetSize` is `"4"`. That is, each worker pod should not run more than `"4"` application jobs.<br>
<br>
For example, when the `"number_of_jobs"` is `"20"`, KEDA should automatically scale the worker StatefulSet to (20 / 4 =) `"5"` pods.
<br>
In this specific example the polling interval (`pollingInterval`) is set to `10` seconds in helm [values.yaml](external-scaler-grpc/helm/external-scaler-server/values.yaml).
<br>
<br>
Let’s put some load on the application by running the following command for creating `"20"` application jobs:
```
kubectl exec -n external-scaler-server -it svc/external-scaler-web -- curl -X POST http://external-scaler-web.external-scaler-web.external-scaler-server.svc.cluster.local:8080/external-scaler-web/api/jobs?number_of_jobs=20
```
The `"number_of_jobs"` query parameter is controlling how many application jobs should be created.
<br>
You may now run the following and watch KEDA auto scaling in action:
```
kubectl -n external-scaler-server get ScaledObject
kubectl -n external-scaler-server get HorizontalPodAutoScaler
kubectl -n external-scaler-server get pods -w
```
In a different terminal window, you may run the following command to view the gRPC application server log:
```
kubectl -n external-scaler-server logs -f -l name=external-scaler-server
```

This example is scaling the number of worker pods (`replicas`) based on the value of an application metric in real time.<br>
<br>
The external-scaler-server pod is running a gRPC server that is responding to the periodic `getMetricSpec` and `getMetrics` requests that are generated by the external KEDA ScaledObject.<br>
<br>
The docker log of the external-scaler-server pod is showing the response for the periodic `getMetricSpec` and `getMetrics` requests that are generated by the external KEDA ScaledObject.
<br>
<br>
The `getMetricSpec` response specifies the name of custom application metric that should control the auto scaling and its target size.<br>
<br>
The periodic `getMetrics` response specifies the value of the custom application metric.<br>
<br>
The external KEDA ScaledObject is dividing the returned value of the custom application metric by its target size ( `"4"` ) - and set the number of worker pods (`replicas`) accordingly.<br>
For example, `metricValue: "20"` and `targetSize: "4"` will set the worker `replicas` to 5 ( `"20"` applications jobs require 5 worker pods ).
Each workder pod should not run more than `"4"` (`targetSize`) application jobs.
<br>
<br>
The worker pods are running each of the application jobs for a random period of time. When each job is completed, the worker is sending a rest call to the external scaler web for deleting the job (reducing the value of the `"number_of_jobs"` metric by 1).
<br>
<br>
In different terminal windows you may run the following commands and view the log of any of the worker pods (running the application jobs) or the external scaler web (managing the number of jobs):
```
kubectl logs -n external-scaler-server -f worker-0
```
```
kubectl logs -n external-scaler-server -f -l name=external-scaler-web
```

The `targetSize` (`numberOfJobsPerServer`), `minimumJobDuration` and `maximumJobDuration` are controlled by the following helm [values.yaml](external-scaler-grpc/helm/external-scaler-server/values.yaml):
```
externalScalerServer:
  numberOfJobsPerServer: "4"
  minimumJobDuration: "10"
  maximumJobDuration: "20"
```
 
### Clean-up
Use the following commands to delete resources created during this post:
```
helm uninstall external-scaler-server --namespace external-scaler-server
```
```
helm uninstall keda --namespace keda
```

For more information, refer to KEDA
[keda](https://keda.sh/).
