package com.asadmshah.hnclone.cache

import dagger.Module
import dagger.Provides
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
internal class CacheModule {

    @Provides
    @Singleton
    internal fun providesCache(cache: CacheImpl): Cache {
        return cache
    }

    @Provides
    @Singleton
    internal fun providesBlockedSessionsCache(cache: Cache, configuration: Configuration): BlockedSessionsCache {
        val expiration = configuration.getLong("auth.request.expire", 10)
        return BlockedSessionsCacheImpl(cache, expiration, TimeUnit.MINUTES)
    }

}