package com.asadmshah.hnclone.cache

import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.RedisURI
import com.lambdaworks.redis.api.StatefulRedisConnection
import org.apache.commons.configuration2.Configuration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

internal class CacheImpl
@Inject
constructor(configuration: Configuration) : Cache {

    private val client: RedisClient
    private val conn: StatefulRedisConnection<String, String>

    init {
        val uri = RedisURI.Builder
                .redis(configuration.getString("redis.host", "localhost"), configuration.getInt("redis.port", 6379))
                .withDatabase(configuration.getInt("redis.db", 0))
                .withPassword(configuration.getString("redis.pass", ""))
                .build()

        client = RedisClient.create(uri)
        conn = client.connect()
    }

    override fun stop() {
        conn.sync().close()
    }

    override fun put(z: Zone, k: String, v: Long) {
        conn.sync().set(z.key(k), v.toString())
    }

    override fun put(z: Zone, k: String, v: Long, exp: LocalDateTime) {
        val seconds = exp.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        conn.sync().setex(z.key(k), seconds, v.toString())
    }

    override fun put(z: Zone, k: String, v: LocalDateTime) {
        conn.sync().set(z.key(k), v.toEpochSecond(ZoneOffset.UTC).toString())
    }

    override fun put(z: Zone, k: String, v: LocalDateTime, exp: LocalDateTime) {
        val seconds = exp.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        conn.sync().setex(z.key(k), seconds, v.toEpochSecond(ZoneOffset.UTC).toString())
    }

    override fun getLong(z: Zone, k: String): Long? {
        return conn.sync().get(z.key(k))?.toLong()
    }

    override fun getLocalDateTime(z: Zone, k: String): LocalDateTime? {
        return conn.sync().get(z.key(k))?.let {
            LocalDateTime.ofInstant(Instant.ofEpochSecond(it.toLong()), ZoneOffset.UTC)
        }
    }
}