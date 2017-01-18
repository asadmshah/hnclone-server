package com.asadmshah.hnclone.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class BlockedSessionsCacheImpl @Inject constructor(expireDuration: Long, expireUnit: TimeUnit) : BlockedSessionsCache {

    private val cache: Cache<Int, LocalDateTime> = Caffeine
            .newBuilder()
            .expireAfterWrite(expireDuration, expireUnit)
            .build<Int, LocalDateTime>()

    override fun put(id: Int) {
        cache.put(id, LocalDateTime.now())
    }

    override fun contains(id: Int, issued: LocalDateTime): Boolean {
        val dt = cache.getIfPresent(id)
        return dt != null && issued.isBefore(dt)
    }
}