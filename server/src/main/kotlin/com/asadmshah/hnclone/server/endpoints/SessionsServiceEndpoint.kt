package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.sessions.ExpiredTokenException
import com.asadmshah.hnclone.common.sessions.InvalidTokenException
import com.asadmshah.hnclone.common.sessions.SessionManager
import com.asadmshah.hnclone.models.SessionCreateRequest
import com.asadmshah.hnclone.models.SessionCreateResponse
import com.asadmshah.hnclone.models.SessionRefreshRequest
import com.asadmshah.hnclone.models.SessionRefreshResponse
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.database.UserExistsException
import com.asadmshah.hnclone.server.database.UsersDatabase
import com.asadmshah.hnclone.services.SessionsServiceGrpc
import io.grpc.Status
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

    override fun refresh(request: SessionRefreshRequest, responseObserver: StreamObserver<SessionRefreshResponse>) {
        try {
            val session = sessions.parseRefreshToken(request.token)
            val refresh = sessions.createRequestToken(session.id)
            val response = SessionRefreshResponse.newBuilder().setToken(refresh).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: ExpiredTokenException) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Expired Token").asRuntimeException())
        } catch (e: InvalidTokenException) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Invalid Token").asRuntimeException())
        }
    }

    override fun create(request: SessionCreateRequest, responseObserver: StreamObserver<SessionCreateResponse>) {
        try {
            val user = usersDatabase.read(request.username, request.password)
            if (user == null) {
                responseObserver.onError(Status.INTERNAL.withDescription("An error occurred. Unable to create account.").asRuntimeException())
                return
            }

            val sreq = sessions.createRequestToken(user.id)
            val sref = sessions.createRefreshToken(user.id)
            val response = SessionCreateResponse.newBuilder().setRequest(sreq).setRefresh(sref).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: UserExistsException) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Username exists").asRuntimeException())
        } catch (e: SQLException) {
            responseObserver.onError(Status.INTERNAL.withDescription("An error occurred. A SQL error occurred.").asRuntimeException())
        }
    }
}