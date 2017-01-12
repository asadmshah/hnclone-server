package com.asadmshah.hnclone.client;

import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

final class TestBaseClientImpl implements BaseClient {

    private static final String SERVER_NAME = "in-process server for " + TestBaseClientImpl.class.getName();

    private final Server server;
    private final ManagedChannel channel;

    TestBaseClientImpl(BindableService... services) throws IOException {
        InProcessServerBuilder builder = InProcessServerBuilder.forName(SERVER_NAME);
        for (BindableService service : services) {
            builder.addService(service);
        }
        builder.directExecutor();

        server = builder.build();
        server.start();

        channel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();
    }

    TestBaseClientImpl(ServerServiceDefinition... services) throws IOException {
        InProcessServerBuilder builder = InProcessServerBuilder.forName(SERVER_NAME);
        for (ServerServiceDefinition service : services) {
            builder.addService(service);
        }
        builder.directExecutor();

        server = builder.build();
        server.start();

        channel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();
    }

    @NotNull
    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean isShutdown() {
        return channel.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return channel.isTerminated();
    }

    @Override
    public void shutdown() {
        channel.shutdownNow();
        try {
            channel.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        server.shutdownNow();
        try {
            server.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
