package com.asadmshah.hnclone.cache

import java.time.LocalDateTime

interface Cache {

    fun stop()

    fun put(z: Zone, k: String, v: Long)

    fun put(z: Zone, k: String, v: Long, exp: LocalDateTime)

    fun put(z: Zone, k: String, v: LocalDateTime)

    fun put(z: Zone, k: String, v: LocalDateTime, exp: LocalDateTime)

    fun getLong(z: Zone, k: String): Long?

    fun getLocalDateTime(z: Zone, k: String): LocalDateTime?
}