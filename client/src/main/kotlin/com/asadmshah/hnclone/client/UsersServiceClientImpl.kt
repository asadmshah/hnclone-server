package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.User
import com.asadmshah.hnclone.services.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

internal class
UsersServiceClientImpl(private val sessions: SessionStorage,
                       private val baseClient: BaseClient,
                       private val sessionsClient: SessionsServiceClient) : UsersServiceClient {

    override fun create(username: String, password: String, about: String): Single<User> {
        return create(UserCreateRequest
                .newBuilder()
                .setUsername(username)
                .setPassword(password)
                .setAbout(about)
                .build())
    }

    internal fun create(request: UserCreateRequest): Single<User> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<User>()
        val f2 = baseClient.call(sessions, UsersServiceGrpc.METHOD_CREATE, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun read(id: Int): Single<User> {
        return read(UserReadUsingIDRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun read(request: UserReadUsingIDRequest): Single<User> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<User>()
        val f2 = baseClient.call(sessions, UsersServiceGrpc.METHOD_READ_USING_ID, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun updateAbout(about: String): Single<String> {
        return updateAbout(UserUpdateAboutRequest
                .newBuilder()
                .setAbout(about)
                .build())
    }

    internal fun updateAbout(request: UserUpdateAboutRequest): Single<String> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<UserUpdateAboutResponse>()
        val f2 = baseClient.call(sessions, UsersServiceGrpc.METHOD_UPDATE_ABOUT, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .map { it.about }
                .onStatusRuntimeErrorResumeNext()
    }

    override fun updatePassword(password: String): Completable {
        return updatePassword(UserUpdatePasswordRequest
                .newBuilder()
                .setPassword(password)
                .build())
    }

    internal fun updatePassword(request: UserUpdatePasswordRequest): Completable {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<UserUpdatePasswordResponse>()
        val f2 = baseClient.call(sessions, UsersServiceGrpc.METHOD_UPDATE_PASSWORD, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .toCompletable()
                .onStatusRuntimeErrorResumeNext()
    }

}