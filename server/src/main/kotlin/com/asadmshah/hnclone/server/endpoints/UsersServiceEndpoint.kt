package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.errors.UsersServiceErrors
import com.asadmshah.hnclone.models.*
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.database.UserExistsException
import com.asadmshah.hnclone.server.database.UsersDatabase
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.UsersServiceGrpc
import io.grpc.Context
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
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
        val user: User?
        try {
            user = usersDatabase.create(request.name, request.pass, request.about ?: "")
        } catch (e: UserExistsException) {
            responseObserver.onError(UsersServiceErrors.ExistsException)
            return
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (user == null) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }

    override fun readUsingID(request: UserReadUsingIDRequest, responseObserver: StreamObserver<User>) {
        val user: User?
        try {
            user = usersDatabase.read(request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (user == null) {
            responseObserver.onError(UsersServiceErrors.NotFoundException)
            return
        }

        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }

    override fun readUsingName(request: UserReadUsingNameRequest, responseObserver: StreamObserver<User>) {
        val user: User?
        try {
            user = usersDatabase.read(request.name)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (user == null) {
            responseObserver.onError(UsersServiceErrors.NotFoundException)
            return
        }

        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }

    override fun updateAbout(request: UserUpdateAboutRequest, responseObserver: StreamObserver<User>) {
        val session = SessionInterceptor.KEY_SESSION.get(Context.current())
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val user: User?
        try {
            user = usersDatabase.update(session.id, request.about)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (user == null) {
            responseObserver.onError(UsersServiceErrors.NotFoundException)
            return
        }

        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }

    override fun delete(request: UserDeleteRequest, responseObserver: StreamObserver<User>) {
        val session = SessionInterceptor.KEY_SESSION.get(Context.current())
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val user: User?
        try {
            user = usersDatabase.delete(session.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (user == null) {
            responseObserver.onError(UsersServiceErrors.NotFoundException)
            return
        }

        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }

}