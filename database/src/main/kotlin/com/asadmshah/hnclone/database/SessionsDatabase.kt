package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.RefreshSession
import io.reactivex.Flowable
import java.time.LocalDateTime

interface SessionsDatabase {

    fun create(id: Int, expires: LocalDateTime = LocalDateTime.now().plusDays(90)): RefreshSession?

    fun read(id: Int): Flowable<RefreshSession>

    fun read(uuid: String): RefreshSession?

    fun delete(id: Int): Int

    fun delete(uuid: String): Boolean
}