package com.asadmshah.hnclone.server;

import com.asadmshah.hnclone.pubsub.PubSub;
import com.asadmshah.hnclone.pubsub.PubSubModule;
import dagger.Module;
import dagger.Provides;
import org.apache.commons.configuration2.Configuration;

import javax.inject.Named;
import javax.inject.Singleton;

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

    @Provides
    @Singleton
    public PubSub providesPubSub(@Named(PubSubModule.REDIS) PubSub pubSub) {
        return pubSub;
    }

}
