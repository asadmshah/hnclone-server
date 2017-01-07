package com.asadmshah.hnclone.pubsub

import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class PubSubModule {

    companion object {
        const val REDIS = "redis"
        const val LOCAL = "local"
    }

    @Provides
    @Singleton
    @Named(REDIS)
    internal fun providesRedisPubSub(pubSub: RedisPubSubImpl): PubSub {
        return pubSub
    }

    @Provides
    @Singleton
    @Named(LOCAL)
    internal fun providesLocalPubSub(pubSub: LocalPubSubImpl): PubSub {
        return pubSub
    }

}