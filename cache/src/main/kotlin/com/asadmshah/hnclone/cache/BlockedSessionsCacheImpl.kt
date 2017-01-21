package com.asadmshah.hnclone.cache

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class BlockedSessionsCacheImpl @Inject constructor(private val cache: Cache, expireDuration: Long, expireUnit: TimeUnit) : BlockedSessionsCache {

    private val seconds = expireUnit.toSeconds(expireDuration)

    override fun put(id: Int) {
        cache.put(Zone.BLOCKED_SESSIONS, id.toString(), LocalDateTime.now(), LocalDateTime.now().plusSeconds(seconds))
    }

    override fun contains(id: Int, issued: LocalDateTime): Boolean {
        return cache.getLocalDateTime(Zone.BLOCKED_SESSIONS, id.toString())?.isAfter(issued) ?: false
    }
}