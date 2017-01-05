package com.asadmshah.hnclone.common.sessions

import com.asadmshah.hnclone.models.RefreshSession
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.models.SessionToken


interface SessionManager {

    fun createRequestToken(id: Int): SessionToken

    fun createRequestToken(session: RequestSession): SessionToken

    fun parseRequestToken(token: ByteArray): RequestSession

    fun parseRequestToken(token: SessionToken): RequestSession

    fun createRefreshToken(id: Int): SessionToken

    fun createRefreshToken(session: RefreshSession): SessionToken

    fun parseRefreshToken(token: ByteArray): RefreshSession

    fun parseRefreshToken(token: SessionToken): RefreshSession

}