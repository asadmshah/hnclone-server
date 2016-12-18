package com.asadmshah.hnclone.server;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.configuration2.Configuration;

@Module
public class ServerModule {

    private final Configuration configuration;

    ServerModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Provides
    public Configuration providesConfiguration() {
        return configuration;
    }

}
