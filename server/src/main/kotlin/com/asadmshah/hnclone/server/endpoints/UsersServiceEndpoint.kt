package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.models.*
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.database.UserExistsException
import com.asadmshah.hnclone.server.database.UsersDatabase
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.UsersServiceGrpc
import io.grpc.Context
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.sql.SQLException

class UsersServiceEndpoint private constructor(component: ServerComponent) : UsersServiceGrpc.UsersServiceImplBase() {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): ServerServiceDefinition {
            val endpoint = UsersServiceEndpoint(component)
            val interceptor = SessionInterceptor.create(component)

            return ServerInterceptors.intercept(endpoint, interceptor)
        }
    }

    private val usersDatabase: UsersDatabase

    init {
        this.usersDatabase = component.usersDatabase()
    }

    override fun create(request: UserCreateRequest, responseObserver: StreamObserver<User>) {
        try {
            val user = usersDatabase.create(request.name, request.pass, request.about ?: "")
            if (user == null) {
                responseObserver.onError(Status.INTERNAL.withDescription("An error occurred.").asRuntimeException())
                return
            }
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: UserExistsException) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Username already exists.").asRuntimeException())
        } catch (e: SQLException) {
            responseObserver.onError(Status.INTERNAL.withDescription("An error occurred.").asRuntimeException())
        }
    }

    override fun readUsingID(request: UserReadUsingIDRequest, responseObserver: StreamObserver<User>) {
        try {
            val user = usersDatabase.read(request.id)
            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found.").asRuntimeException())
                return
            }
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: SQLException) {
            responseObserver.onError(Status.INTERNAL.withDescription("An error occurred.").asRuntimeException())
        }
    }

    override fun readUsingName(request: UserReadUsingNameRequest, responseObserver: StreamObserver<User>) {
        try {
            val user = usersDatabase.read(request.name)
            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found.").asRuntimeException())
                return
            }
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: SQLException) {
            responseObserver.onError(Status.INTERNAL.withDescription("An error occurred.").asRuntimeException())
        }
    }

    override fun updateAbout(request: UserUpdateAboutRequest, responseObserver: StreamObserver<User>) {
        val session = SessionInterceptor.KEY_SESSION.get(Context.current())
        if (session == null) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Unauthenticated").asRuntimeException())
            return
        }

        try {
            val user = usersDatabase.update(session.id, request.about)
            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found.").asRuntimeException())
                return
            }
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: SQLException) {
            responseObserver.onError(Status.INTERNAL.withDescription("An error occurred.").asRuntimeException())
        }
    }

    override fun delete(request: UserDeleteRequest, responseObserver: StreamObserver<User>) {
        val session = SessionInterceptor.KEY_SESSION.get(Context.current())
        if (session == null) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Unauthenticated").asRuntimeException())
            return
        }

        try {
            val user = usersDatabase.delete(session.id)
            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found.").asRuntimeException())
                return
            }
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: SQLException) {
            responseObserver.onError(Status.INTERNAL.withDescription("An error occurred.").asRuntimeException())
            return
        }
    }

}