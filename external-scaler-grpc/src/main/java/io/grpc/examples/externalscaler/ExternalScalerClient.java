package io.grpc.examples.externalscaler;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a from the {@link ExternalScalerServer}.
 */
public class ExternalScalerClient {
    private static final Logger logger = Logger.getLogger(ExternalScalerClient.class.getName());

    private final ExternalScalerGrpc.ExternalScalerBlockingStub blockingStub;

    /**
     * Construct client for accessing ExternalScaler server using the existing channel.
     */
    public ExternalScalerClient(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.

        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        blockingStub = ExternalScalerGrpc.newBlockingStub(channel);
    }

    /**
     * f provided, the first element of {@code args} is the name to use in the
     * . The second argument is the target server.
     */
    public static void main(String[] args) throws Exception {
        String user = "world";
        // Access a service running on the local machine on port 50051
        String target = "localhost:50051";
        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [name [target]]");
                System.err.println("");
                System.err.println("  name    The name you wish to be . Defaults to " + user);
                System.err.println("  target  The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            user = args[0];
        }
        if (args.length > 1) {
            target = args[1];
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();
        try {
            ExternalScalerClient client = new ExternalScalerClient(channel);
            client.isActive(user);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Say hello to server.
     */
    public void isActive(String name) {
        {
            logger.info("Will try to check is active..");
            ScaledObjectRef request = ScaledObjectRef.newBuilder().setName(name).build();
            IsActiveResponse response;
            try {
                response = blockingStub.isActive(request);
            } catch (StatusRuntimeException e) {
                logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
                return;
            }
            logger.info("Is Active: " + response.getResult());
        }
    }
}
