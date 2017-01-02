package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Session
import rx.Observable
import java.time.LocalDateTime

interface SessionsDatabase {

    fun create(id: Int, expires: LocalDateTime = LocalDateTime.now().plusDays(90)): Session?

    fun read(id: Int): Observable<Session>

    fun read(uuid: String): Session?

    fun delete(id: Int): Int

    fun delete(uuid: String): Boolean
}