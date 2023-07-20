package io.grpc.examples.externalscaler;

import com.google.common.net.InetAddresses;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

import javax.naming.CommunicationException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * Server that manages startup/shutdown of a {@code ExternalScaler} server.
 */
public class ExternalScalerServer {
    private static final Logger logger = Logger.getLogger(ExternalScalerServer.class.getName());

    private Server server;

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final ExternalScalerServer server = new ExternalScalerServer();
        server.start();
        server.blockUntilShutdown();
    }

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

    static class ExternalScalerImpl extends ExternalScalerGrpc.ExternalScalerImplBase {

        private static final String NUMBER_OF_JOBS = "number_of_jobs";
        private static final int NUMBER_OF_JOBS_PER_SERVER = 4;

        private static int getNumberOfJobsPerServer() {
            String targetSizeString = System.getenv("NUMBER_OF_JOBS_PER_SERVER");

            int targetSize = Integer.valueOf(targetSizeString);

            if (targetSize == 0) {
                targetSize = NUMBER_OF_JOBS_PER_SERVER;
            }

            System.err.println("NUMBER_OF_JOBS_PER_SERVER [" + targetSize + "]");

            return targetSize;
        }

        private static int getNumberOfJobs(Map<String, String> externalScalerMetadata) {
            int metricValueNumber = 0;

            try {
                final String SERVICE_ORCHESTRATION_URL = "serviceOrchestrationUrl";
                String metricServerUrl = externalScalerMetadata.get(SERVICE_ORCHESTRATION_URL);

                if (metricServerUrl != null) {
                    metricValueNumber = doGetAPICall(metricServerUrl);
                    System.err.println(NUMBER_OF_JOBS + ": [" + metricValueNumber + "]");
                } else {
                    Random random = new Random();
                    int targetSize = getNumberOfJobsPerServer();
                    metricValueNumber = random.nextInt(40 - targetSize) + targetSize;
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

        private void logIsActiveResponse(IsActiveResponse response) {
            System.err.println("\tIsActiveResponse: result [" + response.getResult() + "]");
        }

        @Override
        public void isActive(ScaledObjectRef req, StreamObserver<IsActiveResponse> responseObserver) {
            System.err.println("isActive");

            boolean isActiveValue = true;
            String metricName = NUMBER_OF_JOBS;
            int metricValueNumber = getNumberOfJobs(req.getScalerMetadata());
            if (metricValueNumber == 0) isActiveValue = false;

            logScaledObjectRef(req);

            IsActiveResponse reply = IsActiveResponse.newBuilder().setResult(isActiveValue).build();
            logIsActiveResponse(reply);
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void streamIsActive(ScaledObjectRef req, StreamObserver<IsActiveResponse> responseObserver) {
            System.err.println("streamIsActive");

            boolean isActiveValue = true;
            String metricName = NUMBER_OF_JOBS;
            int metricValueNumber = getNumberOfJobs(req.getScalerMetadata());
            if (metricValueNumber == 0) isActiveValue = false;

            logScaledObjectRef(req);

            IsActiveResponse reply = IsActiveResponse.newBuilder().setResult(isActiveValue).build();
            logIsActiveResponse(reply);
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void getMetricSpec(ScaledObjectRef req, StreamObserver<GetMetricSpecResponse> responseObserver) {
            System.err.println("getMetricSpec");
            logScaledObjectRef(req);

            String metricName = NUMBER_OF_JOBS;
            int targetSize = getNumberOfJobsPerServer();

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
    }
}
