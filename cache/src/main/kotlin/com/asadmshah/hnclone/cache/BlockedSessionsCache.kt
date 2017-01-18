package com.asadmshah.hnclone.cache

import java.time.LocalDateTime

interface BlockedSessionsCache {

    fun put(id: Int)

    fun contains(id: Int, issued: LocalDateTime): Boolean
}