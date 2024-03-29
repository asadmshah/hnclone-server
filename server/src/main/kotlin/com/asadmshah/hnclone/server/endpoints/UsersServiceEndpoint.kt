package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.cache.BlockedSessionsCache
import com.asadmshah.hnclone.common.tools.escape
import com.asadmshah.hnclone.database.UserExistsException
import com.asadmshah.hnclone.database.UsersDatabase
import com.asadmshah.hnclone.errors.*
import com.asadmshah.hnclone.models.User
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.*
import io.grpc.Context
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.stub.StreamObserver
import org.apache.commons.lang3.StringUtils
import java.sql.SQLException
import java.util.regex.Pattern

class UsersServiceEndpoint private constructor(component: ServerComponent) : UsersServiceGrpc.UsersServiceImplBase() {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): ServerServiceDefinition {
            val endpoint = UsersServiceEndpoint(component)
            val interceptor = SessionInterceptor.create(component)

            return ServerInterceptors.intercept(endpoint, interceptor)
        }

        private val patternUsername = Pattern.compile("^\\w{0,32}$")
    }

    private val usersDatabase: UsersDatabase
    private val blockedSessionsCache: BlockedSessionsCache

    init {
        this.usersDatabase = component.usersDatabase()
        this.blockedSessionsCache = component.blockedSessionsCache()
    }

    override fun create(request: UserCreateRequest, responseObserver: StreamObserver<User>) {
        val username = if (request.username.isNullOrBlank()) null else request.username.trim().escape()
        if (username == null) {
            responseObserver.onError(UsernameRequiredStatusException())
            return
        }

        if (StringUtils.containsWhitespace(username)) {
            responseObserver.onError(UsernameInvalidStatusException())
            return
        }

        if (!patternUsername.matcher(username).matches()) {
            responseObserver.onError(UsernameInvalidStatusException())
            return
        }

        val password = if (request.password.isNullOrBlank()) null else request.password.trim().escape()
        if (password == null || password.isBlank()) {
            responseObserver.onError(PasswordInsecureStatusException())
            return
        }

        val about = if (request.about.isNullOrBlank()) "" else request.about.trim().escape()
        if (about.length > 512) {
            responseObserver.onError(UserAboutTooLongStatusException())
            return
        }

        val user: User?
        try {
            user = usersDatabase.create(username, password, about)
        } catch (e: UserExistsException) {
            responseObserver.onError(UsernameExistsStatusException())
            return
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (user == null) {
            responseObserver.onError(UnknownStatusException())
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
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (user == null) {
            responseObserver.onError(UserNotFoundStatusException())
            return
        }

        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }

    override fun readUsingName(request: UserReadUsingNameRequest, responseObserver: StreamObserver<User>) {
        val user: User?
        try {
            user = usersDatabase.read(request.username)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (user == null) {
            responseObserver.onError(UserNotFoundStatusException())
            return
        }

        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }

    override fun updateAbout(request: UserUpdateAboutRequest, responseObserver: StreamObserver<UserUpdateAboutResponse>) {
        val session = SessionInterceptor.KEY_SESSION.get(Context.current())
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val about = if (request.about.isNullOrBlank()) "" else request.about.trim().escape()
        if (about.length > 512) {
            responseObserver.onError(UserAboutTooLongStatusException())
            return
        }

        var response: UserUpdateAboutResponse? = null
        try {
            val s = usersDatabase.updateAbout(session.id, about)
            if (s != null) {
                response = UserUpdateAboutResponse.newBuilder().setAbout(s).build()
            }
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (response == null) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun updatePassword(request: UserUpdatePasswordRequest, responseObserver: StreamObserver<UserUpdatePasswordResponse>) {
        val session = SessionInterceptor.KEY_SESSION.get(Context.current())
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val password = if (request.password.isNullOrBlank()) null else request.password.trim().escape()
        if (password == null || password.isBlank()) {
            responseObserver.onError(PasswordInsecureStatusException())
            return
        }

        var response: UserUpdatePasswordResponse? = null
        try {
            val s = usersDatabase.updatePassword(session.id, password)
            if (s != null && s) {
                response = UserUpdatePasswordResponse.getDefaultInstance()
            }
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (response == null) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        blockedSessionsCache.put(session.id)

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}