package com.asadmshah.hnclone.client;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

final class TestBaseClient extends BaseClientImpl {

    private static final String SERVER_NAME = "in-process server for " + TestBaseClient.class.getName();

    private final Server server;

    private TestBaseClient(@NotNull ManagedChannel channel, Server server) throws IOException {
        super(channel);

        this.server = server;
        this.server.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();

        server.shutdownNow();
        try {
            server.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static BaseClient create(BindableService... services) throws IOException {
        InProcessServerBuilder builder = InProcessServerBuilder.forName(SERVER_NAME);
        for (BindableService service : services) {
            builder.addService(service);
        }
        builder.directExecutor();

        return create(builder.build());
    }

    static BaseClient create(ServerServiceDefinition... services) throws IOException {
        InProcessServerBuilder builder = InProcessServerBuilder.forName(SERVER_NAME);
        for (ServerServiceDefinition service : services) {
            builder.addService(service);
        }
        builder.directExecutor();

        return create(builder.build());
    }

    static BaseClient create(Server server) throws IOException {
        ManagedChannel channel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        return new TestBaseClient(channel, server);
    }


}
