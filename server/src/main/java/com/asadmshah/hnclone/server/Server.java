package com.asadmshah.hnclone.server;

import com.asadmshah.hnclone.server.endpoints.PostsServiceEndpoint;
import com.asadmshah.hnclone.server.endpoints.SessionsServiceEndpoint;
import com.asadmshah.hnclone.server.endpoints.UsersServiceEndpoint;
import io.grpc.ServerBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.io.IOException;

public class Server {

    private final io.grpc.Server grpcServer;

    public Server(ServerComponent component) {
        this.grpcServer = ServerBuilder
                .forPort(component.configuration().getInt("server.port"))
                .addService(SessionsServiceEndpoint.create(component))
                .addService(PostsServiceEndpoint.create(component))
                .addService(UsersServiceEndpoint.create(component))
                .build();
    }

    private void start() throws IOException {
        grpcServer.start();

        System.out.println("Server Started on port " + grpcServer.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                Server.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (grpcServer != null) {
            grpcServer.shutdown();
        }
    }

    private void blockedStop() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configurations().properties(Server.class.getClassLoader().getResource("debug.properties"));

        MetricsServer metricsServer = new MetricsServer(9096);
        metricsServer.start();

        ServerComponent component = DaggerServerComponent.builder()
                .serverModule(new ServerModule(configuration))
                .build();

        final Server server = new Server(component);
        server.start();
        server.blockedStop();

        metricsServer.stop();
    }

}
