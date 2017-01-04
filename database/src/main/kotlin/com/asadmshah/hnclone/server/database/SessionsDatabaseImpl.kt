package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Session
import rx.Observable
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

internal class SessionsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource) : SessionsDatabase {

    override fun create(id: Int, expires: LocalDateTime): Session? {
        val uuidStr = UUID.randomUUID().toString()
        val expiresDouble = expires.toEpochSecond(ZoneOffset.UTC).toDouble()

        return dataSource
                .executeSingle("SELECT * FROM sessions_create($id, '$uuidStr', $expiresDouble);", { it.getSession() })
    }

    override fun read(id: Int): Observable<Session> {
        return dataSource
                .executeObservable("SELECT * FROM sessions_read_id($id);", { it.getSession() })
    }

    override fun read(uuid: String): Session? {
        return dataSource
                .executeSingle("SELECT * FROM sessions_read_uuid('$uuid');", { it.getSession() })
    }

    override fun delete(id: Int): Int {
        return dataSource
                .executeSingle("SELECT * FROM sessions_delete($id);", { it.getInt() }) ?: 0
    }

    override fun delete(uuid: String): Boolean {
        return dataSource
                .executeSingle("SELECT * FROM sessions_delete('$uuid');", { it.getBoolean() }) ?: false
    }
}