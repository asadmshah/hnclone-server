package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.RefreshSession
import io.reactivex.Flowable
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

internal class SessionsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource) : SessionsDatabase {

    override fun create(id: Int, expires: LocalDateTime): RefreshSession? {
        val uuidStr = UUID.randomUUID().toString()
        val expiresDouble = expires.toEpochSecond(ZoneOffset.UTC).toDouble()

        return dataSource
                .executeSingle("SELECT * FROM sessions_create($id, '$uuidStr', $expiresDouble);", ResultSet::getRefreshSession)
    }

    override fun read(id: Int): Flowable<RefreshSession> {
        return dataSource
                .executeFlowable("SELECT * FROM sessions_read_id($id);", ResultSet::getRefreshSession)
    }

    override fun read(uuid: String): RefreshSession? {
        return dataSource
                .executeSingle("SELECT * FROM sessions_read_uuid('$uuid');", ResultSet::getRefreshSession)
    }

    override fun delete(id: Int): Int {
        return dataSource
                .executeSingle("SELECT * FROM sessions_delete($id);", ResultSet::getInt) ?: 0
    }

    override fun delete(uuid: String): Boolean {
        return dataSource
                .executeSingle("SELECT * FROM sessions_delete('$uuid');", ResultSet::getBoolean) ?: false
    }
}