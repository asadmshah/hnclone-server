package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.cache.BlockedSessionsCache
import com.asadmshah.hnclone.common.sessions.ExpiredTokenException
import com.asadmshah.hnclone.common.sessions.InvalidTokenException
import com.asadmshah.hnclone.common.sessions.SessionManager
import com.asadmshah.hnclone.database.SessionsDatabase
import com.asadmshah.hnclone.database.UsersDatabase
import com.asadmshah.hnclone.errors.SessionExpiredTokenStatusException
import com.asadmshah.hnclone.errors.SessionInvalidTokenStatusException
import com.asadmshah.hnclone.errors.UnknownStatusException
import com.asadmshah.hnclone.errors.UserNotFoundStatusException
import com.asadmshah.hnclone.models.SessionToken
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.services.SessionCreateRequest
import com.asadmshah.hnclone.services.SessionCreateResponse
import com.asadmshah.hnclone.services.SessionsServiceGrpc
import io.grpc.stub.StreamObserver
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionsServiceEndpoint private constructor(component: ServerComponent) : SessionsServiceGrpc.SessionsServiceImplBase() {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): SessionsServiceEndpoint {
            return SessionsServiceEndpoint(component)
        }
    }

    private val usersDatabase: UsersDatabase
    private val sessionsDatabase: SessionsDatabase
    private val sessions: SessionManager
    private val blockedSessionsCache: BlockedSessionsCache

    init {
        this.usersDatabase = component.usersDatabase()
        this.sessionsDatabase = component.sessionsDatabase()
        this.sessions = component.sessionManager()
        this.blockedSessionsCache = component.blockedSessionsCache()
    }

    override fun refresh(request: SessionToken, responseObserver: StreamObserver<SessionToken>) {
        try {
            val session = sessions.parseRefreshToken(request)

            val issued = LocalDateTime.ofInstant(Instant.ofEpochMilli(session.issued), ZoneOffset.UTC)
            if (blockedSessionsCache.contains(session.id, issued)) {
                responseObserver.onError(SessionInvalidTokenStatusException())
                return
            }

            if (sessionsDatabase.read(session.uuid) == null) {
                responseObserver.onError(SessionInvalidTokenStatusException())
                return
            }
            responseObserver.onNext(sessions.createRequestToken(session.id))
            responseObserver.onCompleted()
        } catch (e: ExpiredTokenException) {
            responseObserver.onError(SessionExpiredTokenStatusException())
        } catch (e: InvalidTokenException) {
            responseObserver.onError(SessionInvalidTokenStatusException())
        }
    }

    override fun create(request: SessionCreateRequest, responseObserver: StreamObserver<SessionCreateResponse>) {
        try {
            val user = usersDatabase.read(request.username, request.password)
            if (user == null) {
                responseObserver.onError(UserNotFoundStatusException())
                return
            }

            val sreq = sessions.createRequestToken(user.id)
            val sref = sessions.createRefreshToken(user.id)

            val response = SessionCreateResponse.newBuilder().setRequest(sreq).setRefresh(sref).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
        }
    }
}