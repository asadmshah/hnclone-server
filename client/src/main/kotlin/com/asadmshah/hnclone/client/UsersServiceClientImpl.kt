package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.errors.UsersServiceErrors
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.models.User
import com.asadmshah.hnclone.services.*
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit

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
                .onErrorResumeNext { it: Throwable ->
                    if (it is StatusRuntimeException) {
                        Single.error<User>(restoreException(it))
                    } else {
                        Single.error<User>(it)
                    }
                }
    }

    override fun read(request: UserReadUsingIDRequest): Single<User> {
        return Single
                .fromCallable {
                    val stub = UsersServiceGrpc.newBlockingStub(base.getChannel())
                    stub.readUsingID(request)
                }
                .onErrorResumeNext {
                    if (it is StatusRuntimeException) {
                        Single.error<User>(restoreException(it))
                    } else {
                        Single.error<User>(it)
                    }
                }
    }

    override fun read(request: UserReadUsingNameRequest): Single<User> {
        return Single
                .fromCallable {
                    val stub = UsersServiceGrpc.newBlockingStub(base.getChannel())
                    stub.readUsingName(request)
                }
                .onErrorResumeNext {
                    if (it is StatusRuntimeException) {
                        Single.error<User>(restoreException(it))
                    } else {
                        Single.error<User>(it)
                    }
                }
    }

    override fun update(request: UserUpdateAboutRequest): Single<String> {
        return Observable
                .fromCallable {
                    val sk = sessionsStore.getRequestKey()
                    if (sk == null) {
                        throw CommonServiceErrors.UNAUTHENTICATED_EXCEPTION
                    } else {
                        val rk = RequestSession.parseFrom(sk.data)
                        val dt = TimeUnit.SECONDS.toMillis(rk.expire)
                        if (dt < System.currentTimeMillis()) {
                            sessionsClient.refresh().blockingAwait()
                        }
                    }
                    ""
                }
                .map {
                    val md = io.grpc.Metadata()
                    sessionsStore.getRequestKey()?.let {
                        md.put(AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(UsersServiceGrpc.newBlockingStub(base.getChannel()), md)
                    stub.updateAbout(request).about
                }
                .singleOrError()
                .onErrorResumeNext {
                    if (it is StatusRuntimeException) {
                        Single.error<String>(restoreException(it))
                    } else {
                        Single.error<String>(it)
                    }
                }
    }

    internal fun restoreException(exception: StatusRuntimeException): StatusRuntimeException {
        val restored = when (exception.status.description) {
            UsersServiceErrors.USERNAME_REQUIRED.description -> UsersServiceErrors.USERNAME_REQUIRED_EXCEPTION
            UsersServiceErrors.USERNAME_INVALID.description -> UsersServiceErrors.USERNAME_INVALID_EXCEPTION
            UsersServiceErrors.PASSWORD_INSECURE.description -> UsersServiceErrors.PASSWORD_INSECURE_EXCEPTION
            UsersServiceErrors.ABOUT_TOO_LONG.description -> UsersServiceErrors.ABOUT_TOO_LONG_EXCEPTION
            UsersServiceErrors.USERNAME_EXISTS.description -> UsersServiceErrors.USERNAME_EXISTS_EXCEPTION
            UsersServiceErrors.NOT_FOUND.description -> UsersServiceErrors.NOT_FOUND_EXCEPTION
            CommonServiceErrors.UNKNOWN.description -> CommonServiceErrors.UNKNOWN_EXCEPTION
            CommonServiceErrors.UNAUTHENTICATED.description -> CommonServiceErrors.UNAUTHENTICATED_EXCEPTION
            CommonServiceErrors.UNAUTHORIZED.description -> CommonServiceErrors.UNAUTHORIZED_EXCEPTION
            else -> exception
        }
        return restored
    }

}