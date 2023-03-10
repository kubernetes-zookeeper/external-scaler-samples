/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.externalscaler;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.net.URISyntaxException;
import javax.naming.CommunicationException;
import com.google.common.net.InetAddresses;


/**
 * Server that manages startup/shutdown of a {@code ExternalScaler} server.
 */
public class ExternalScalerServer {
  private static final Logger logger = Logger.getLogger(ExternalScalerServer.class.getName());

  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    String address = "0.0.0.0";
    int port = 50051;
    server = NettyServerBuilder.forAddress(new InetSocketAddress(InetAddresses.forString(address), port))
        .addService(new ExternalScalerImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + address + ":" + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          ExternalScalerServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final ExternalScalerServer server = new ExternalScalerServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class ExternalScalerImpl extends ExternalScalerGrpc.ExternalScalerImplBase {

    private static final String NUMBER_OF_JOBS = "number_of_jobs";
    private static final int NUMBER_OF_JOBS_PER_SERVER = 4;

    private void logScaledObjectRef(ScaledObjectRef req) {
      System.err.println("\tScaledObjectRef: name [" + req.getName() + "]");
      System.err.println("\tScaledObjectRef: namespace [" + req.getNamespace() + "]");
      Map<String, String> scalerMetadata = req.getScalerMetadata();
      System.err.println("\tScaledObjectRef: external scaler metadata [" + scalerMetadata + "]");
    }

    private void logGetMetricsRequest(GetMetricsRequest req) {
      logScaledObjectRef(req.getScaledObjectRef());
      System.err.println("\tGetMetricsRequest: metricName [" + req.getMetricName() + "]");
    }

    private void logGetMetricsResponse(GetMetricsResponse response) {
      List<MetricValue> metricValuesList = response.getMetricValuesList();
      System.err.println("\tGetMetricsResponse: MetricValuesList [" + metricValuesList + "]");
    }

    private void logGetMetricSpecResponse(GetMetricSpecResponse response) {
      List<MetricSpec> metricSpecList = response.getMetricSpecsList();
      System.err.println("\tGetMetricSpecResponse: metricSpecList [" + metricSpecList + "]");
    }

    @Override
    public void isActive(ScaledObjectRef req, StreamObserver<IsActiveResponse> responseObserver) {
      System.err.println("isActive");
      logScaledObjectRef(req);

      IsActiveResponse reply = IsActiveResponse.newBuilder().setResult(true).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void streamIsActive(ScaledObjectRef req, StreamObserver<IsActiveResponse> responseObserver) {
      System.err.println("streamIsActive");
      logScaledObjectRef(req);

      IsActiveResponse reply = IsActiveResponse.newBuilder().setResult(true).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void getMetricSpec(ScaledObjectRef req, StreamObserver<GetMetricSpecResponse> responseObserver) {
      System.err.println("getMetricSpec");
      logScaledObjectRef(req);

      String metricName = NUMBER_OF_JOBS;
      int targetSize = NUMBER_OF_JOBS_PER_SERVER;

      MetricSpec metricSpec = MetricSpec.newBuilder().setMetricName(metricName).setTargetSize(targetSize).build();
      GetMetricSpecResponse reply = GetMetricSpecResponse.newBuilder().addMetricSpecs(metricSpec).build();
      System.err.println("getMetricSpec Response");
      logGetMetricSpecResponse(reply);
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void getMetrics(GetMetricsRequest req, StreamObserver<GetMetricsResponse> responseObserver) {
      System.err.println("getMetrics");
      logGetMetricsRequest(req);

      String metricName = NUMBER_OF_JOBS;

      int metricValueNumber = getNumberOfJobs(req.getScaledObjectRef().getScalerMetadata());

      MetricValue metricValue = MetricValue.newBuilder().setMetricName(metricName).setMetricValue(metricValueNumber).build();
      GetMetricsResponse reply = GetMetricsResponse.newBuilder().addMetricValues(metricValue).build();
      System.err.println("getMetrics Response");
      logGetMetricsResponse(reply);
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    private static int getNumberOfJobs(Map<String, String> externalScalerMetadata) {
      int metricValueNumber = 0;

      try
      {
	  final String SERVICE_ORCHESTRATION_URL = "serviceOrchestrationUrl";
	  String metricServerUrl = externalScalerMetadata.get(SERVICE_ORCHESTRATION_URL);

	  if ( metricServerUrl != null ) {
            metricValueNumber = doGetAPICall(metricServerUrl);
  	    System.err.println(NUMBER_OF_JOBS + ": [" + metricValueNumber + "]");
	  } else {
            Random random = new Random();
            metricValueNumber = random.nextInt(40 - NUMBER_OF_JOBS_PER_SERVER) + NUMBER_OF_JOBS_PER_SERVER;
  	    System.err.println("Failed to get [" + SERVICE_ORCHESTRATION_URL + "] from External Scaler Metadata. Using random [" + metricValueNumber + "]...");
	  }

      } catch (Exception e) {
	      System.err.println("Exception: " + e);
      }
      return metricValueNumber;
    }

    private static Integer doGetAPICall(String path) throws URISyntaxException, IOException, CommunicationException {

        Integer response = 0;

        boolean trustAnySSLCertificate = true;

        response = HttpClientUtils.executeGetRequest(path, trustAnySSLCertificate, Integer.class, null);

        return response;
    }
  }
}
