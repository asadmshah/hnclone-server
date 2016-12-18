package com.asadmshah.hnclone.server;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

final class MetricsServer {

    private final org.eclipse.jetty.server.Server server;

    MetricsServer(int port) {
        this.server = new Server(port);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
    }

    void start() throws Exception {
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down metrics server since JVM is shutting down");
                try {
                    server.stop();
                } catch (Exception ignored) {

                }
                System.err.println("*** metrics server shut down");
            }
        });

        DefaultExports.initialize();
    }

    void stop() throws Exception {
        if (server.isStarted() || server.isStarting()) {
            server.stop();
        }
    }

}
