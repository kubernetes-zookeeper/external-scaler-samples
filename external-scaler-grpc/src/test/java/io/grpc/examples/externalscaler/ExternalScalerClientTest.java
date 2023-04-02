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

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ExternalScalerClient}.
 * For demonstrating how to write gRPC unit test only.
 * Not intended to provide a high code coverage or to test every major usecase.
 * <p>
 * directExecutor() makes it easier to have deterministic tests.
 * However, if your implementation uses another thread and uses streaming it is better to use
 * the default executor, to avoid hitting bug #3084.
 *
 * <p>For more unit test examples see {@link io.grpc.examples.routeguide.RouteGuideClientTest} and
 * {@link io.grpc.examples.routeguide.RouteGuideServerTest}.
 */
@RunWith(JUnit4.class)
public class ExternalScalerClientTest {
    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final ExternalScalerGrpc.ExternalScalerImplBase serviceImpl =
            mock(ExternalScalerGrpc.ExternalScalerImplBase.class, delegatesTo(
                    new ExternalScalerGrpc.ExternalScalerImplBase() {
                        // By default the client will receive Status.UNIMPLEMENTED for all RPCs.
                        // You might need to implement necessary behaviors for your test here, like this:
                        //
                        // @Override
                        // public void isActive(Request request, StreamObserver<Reply> respObserver) {
                        //   respObserver.onNext(Reply.getDefaultInstance());
                        //   respObserver.onCompleted();
                        // }
                    }));

    private ExternalScalerClient client;

    @Before
    public void setUp() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

        // Create a ExternalScalerClient using the in-process channel;
        client = new ExternalScalerClient(channel);
    }

    /**
     * To test the client, call from the client against the fake server, and verify behaviors or state
     * changes from the server side.
     */
    @Test
    public void externalScaler_messageDeliveredToServer() {
        ArgumentCaptor<ScaledObjectRef> requestCaptor = ArgumentCaptor.forClass(ScaledObjectRef.class);

        client.isActive("test name");

        verify(serviceImpl)
                .isActive(requestCaptor.capture(), ArgumentMatchers.<StreamObserver<IsActiveResponse>>any());
        assertEquals("test name", requestCaptor.getValue().getName());
    }
}
