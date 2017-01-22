package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.User
import com.asadmshah.hnclone.services.*
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.reactivex.Completable
import io.reactivex.Single

internal class
UsersServiceClientImpl(private val sessionsStore: SessionStorage,
                       private val base: BaseClient,
                       private val sessionsClient: SessionsServiceClient) : UsersServiceClient {

    companion object {
        private val AUTHORIZATION_KEY = Metadata.Key.of("authorization-bin", Metadata.BINARY_BYTE_MARSHALLER)
    }

    override fun create(request: UserCreateRequest): Single<User> {
        return Single
                .fromCallable {
                    val stub = UsersServiceGrpc.newBlockingStub(base.getChannel())
                    stub.create(request)
                }
                .onStatusRuntimeErrorResumeNext()
    }

    override fun read(request: UserReadUsingIDRequest): Single<User> {
        return Single
                .fromCallable {
                    val stub = UsersServiceGrpc.newBlockingStub(base.getChannel())
                    stub.readUsingID(request)
                }
                .onStatusRuntimeErrorResumeNext()
    }

    override fun read(request: UserReadUsingNameRequest): Single<User> {
        return Single
                .fromCallable {
                    val stub = UsersServiceGrpc.newBlockingStub(base.getChannel())
                    stub.readUsingName(request)
                }
                .onStatusRuntimeErrorResumeNext()
    }

    override fun update(request: UserUpdateAboutRequest): Single<String> {
        return sessionsClient
                .refresh()
                .andThen(justUpdate(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justUpdate(request: UserUpdateAboutRequest): Single<String> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessionsStore.getRequestKey()?.let {
                        md.put(AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(UsersServiceGrpc.newBlockingStub(base.getChannel()), md)
                    stub.updateAbout(request).about
                }
    }

    override fun update(request: UserUpdatePasswordRequest): Completable {
        return sessionsClient
                .refresh()
                .andThen(justUpdate(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justUpdate(request: UserUpdatePasswordRequest): Completable {
        return Completable
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessionsStore.getRequestKey()?.let {
                        md.put(AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(UsersServiceGrpc.newBlockingStub(base.getChannel()), md)
                    stub.updatePassword(request)
                }
    }

    override fun delete(): Completable {
        return sessionsClient
                .refresh()
                .andThen(justDelete())
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justDelete(): Completable {
        return Completable
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessionsStore.getRequestKey()?.let {
                        md.put(AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(UsersServiceGrpc.newBlockingStub(base.getChannel()), md)
                    stub.delete(UserDeleteRequest.getDefaultInstance()).deleted
                }
    }

}