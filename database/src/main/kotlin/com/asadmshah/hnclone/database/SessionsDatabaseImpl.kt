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

    private companion object {
        // language=PostgreSQL
        const val SQL_CREATE = "SELECT * FROM sessions_create(?, ?, ?);"
        // language=PostgreSQL
        const val SQL_READ_ID = "SELECT * FROM sessions_read_id(?);"
        // language=PostgreSQL
        const val SQL_READ_UUID = "SELECT * FROM sessions_read_uuid(?);"
        // language=PostgreSQL
        const val SQL_DELETE_ID = "SELECT * FROM sessions_delete_id(?);"
        // language=PostgreSQL
        const val SQL_DELETE_UUID = "SELECT * FROM sessions_delete_uuid(?);"
    }

    override fun create(id: Int, expires: LocalDateTime): RefreshSession? {
        return dataSource.executeSingle(SQL_CREATE, {
            it.setInt(1, id)
            it.setString(2, UUID.randomUUID().toString())
            it.setDouble(3, expires.toEpochSecond(ZoneOffset.UTC).toDouble())
        }, ResultSet::getRefreshSession)
    }

    override fun read(id: Int): Flowable<RefreshSession> {
        return dataSource.executeFlowable(SQL_READ_ID, {
            it.setInt(1, id)
        }, ResultSet::getRefreshSession)
    }

    override fun read(uuid: String): RefreshSession? {
        return dataSource.executeSingle(SQL_READ_UUID, {
            it.setString(1, uuid)
        }, ResultSet::getRefreshSession)
    }

    override fun delete(id: Int): Int {
        return dataSource.executeSingle(SQL_DELETE_ID, {
            it.setInt(1, id)
        }, ResultSet::getInt) ?: 0
    }

    override fun delete(uuid: String): Boolean {
        return dataSource.executeSingle(SQL_DELETE_UUID, {
            it.setString(1, uuid)
        }, ResultSet::getBoolean) ?: false
    }
}