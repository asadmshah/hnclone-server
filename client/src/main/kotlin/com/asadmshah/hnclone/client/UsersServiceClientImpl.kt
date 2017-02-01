package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.User
import com.asadmshah.hnclone.services.*
import io.grpc.stub.MetadataUtils
import io.reactivex.Completable
import io.reactivex.Single

internal class
UsersServiceClientImpl(private val sessionsStore: SessionStorage,
                       private val base: BaseClient,
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
        return Single
                .fromCallable {
                    val stub = UsersServiceGrpc.newBlockingStub(base.getChannel())
                    stub.create(request)
                }
                .onStatusRuntimeErrorResumeNext()
    }

    override fun read(id: Int): Single<User> {
        return read(UserReadUsingIDRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun read(request: UserReadUsingIDRequest): Single<User> {
        return Single
                .fromCallable {
                    val stub = UsersServiceGrpc.newBlockingStub(base.getChannel())
                    stub.readUsingID(request)
                }
                .onStatusRuntimeErrorResumeNext()
    }

    override fun updateAbout(about: String): Single<String> {
        return update(UserUpdateAboutRequest
                .newBuilder()
                .setAbout(about)
                .build())
    }

    internal fun update(request: UserUpdateAboutRequest): Single<String> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justUpdate(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justUpdate(request: UserUpdateAboutRequest): Single<String> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessionsStore.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(UsersServiceGrpc.newBlockingStub(base.getChannel()), md)
                    stub.updateAbout(request).about
                }
    }

    override fun updatePassword(password: String): Completable {
        return update(UserUpdatePasswordRequest
                .newBuilder()
                .setPassword(password)
                .build())
    }

    internal fun update(request: UserUpdatePasswordRequest): Completable {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justUpdate(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justUpdate(request: UserUpdatePasswordRequest): Completable {
        return Completable
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessionsStore.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(UsersServiceGrpc.newBlockingStub(base.getChannel()), md)
                    stub.updatePassword(request)
                }
    }

}