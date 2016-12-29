package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.sessions.ExpiredTokenException
import com.asadmshah.hnclone.common.sessions.InvalidTokenException
import com.asadmshah.hnclone.common.sessions.SessionManager
import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.errors.SessionsServiceErrors
import com.asadmshah.hnclone.models.SessionCreateRequest
import com.asadmshah.hnclone.models.SessionCreateResponse
import com.asadmshah.hnclone.models.SessionToken
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.database.UsersDatabase
import com.asadmshah.hnclone.services.SessionsServiceGrpc
import io.grpc.stub.StreamObserver
import java.sql.SQLException

class SessionsServiceEndpoint private constructor(component: ServerComponent) : SessionsServiceGrpc.SessionsServiceImplBase() {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): SessionsServiceEndpoint {
            return SessionsServiceEndpoint(component)
        }
    }

    private val usersDatabase: UsersDatabase
    private val sessions: SessionManager

    init {
        this.usersDatabase = component.usersDatabase()
        this.sessions = component.sessionManager()
    }

    override fun refresh(request: SessionToken, responseObserver: StreamObserver<SessionToken>) {
        try {
            val session = sessions.parseRefreshToken(request)
            responseObserver.onNext(sessions.createRequestToken(session.id))
            responseObserver.onCompleted()
        } catch (e: ExpiredTokenException) {
            responseObserver.onError(SessionsServiceErrors.ExpiredTokenException)
        } catch (e: InvalidTokenException) {
            responseObserver.onError(SessionsServiceErrors.InvalidTokenException)
        }
    }

    override fun create(request: SessionCreateRequest, responseObserver: StreamObserver<SessionCreateResponse>) {
        try {
            val user = usersDatabase.read(request.username, request.password)
            if (user == null) {
                responseObserver.onError(SessionsServiceErrors.UserNotFoundException)
                return
            }

            val sreq = sessions.createRequestToken(user.id)
            val sref = sessions.createRefreshToken(user.id)

            val response = SessionCreateResponse.newBuilder().setRequest(sreq).setRefresh(sref).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
        }
    }
}