package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.RefreshSession
import rx.Observable
import java.time.LocalDateTime

interface SessionsDatabase {

    fun create(id: Int, expires: LocalDateTime = LocalDateTime.now().plusDays(90)): RefreshSession?

    fun read(id: Int): Observable<RefreshSession>

    fun read(uuid: String): RefreshSession?

    fun delete(id: Int): Int

    fun delete(uuid: String): Boolean
}