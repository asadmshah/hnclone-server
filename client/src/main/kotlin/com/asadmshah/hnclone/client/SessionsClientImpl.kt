package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.errors.SessionsServiceErrors
import com.asadmshah.hnclone.services.SessionCreateRequest
import com.asadmshah.hnclone.services.SessionsServiceGrpc
import io.grpc.StatusRuntimeException
import io.reactivex.Completable

internal class
SessionsClientImpl(private val sessions: SessionStorage,
                   private val baseClient: BaseClient) : SessionsClient {

    override fun refresh(): Completable {
        return Completable
                .fromCallable {
                    val stub = SessionsServiceGrpc.newBlockingStub(baseClient.getChannel())
                    val response = stub.refresh(sessions.getRefreshKey())
                    sessions.putRequestKey(response)
                    0
                }
                .onErrorResumeNext {
                    val throwable: Throwable
                    if (it is StatusRuntimeException) {
                        when (it.status.description) {
                            SessionsServiceErrors.EXPIRED_TOKEN.description -> {
                                throwable = SessionsServiceErrors.EXPIRED_TOKEN_EXCEPTION
                            }
                            SessionsServiceErrors.INVALID_TOKEN.description -> {
                                throwable = SessionsServiceErrors.INVALID_TOKEN_EXCEPTION
                            }
                            else -> {
                                throwable = it
                            }
                        }
                    } else {
                        throwable = it
                    }
                    Completable.error(throwable)
                }
    }

    override fun create(request: SessionCreateRequest): Completable {
        return Completable
                .fromCallable {
                    val stub = SessionsServiceGrpc.newBlockingStub(baseClient.getChannel())
                    val response = stub.create(request)
                    sessions.putRefreshKey(response.refresh)
                    sessions.putRequestKey(response.request)
                    0
                }
                .onErrorResumeNext {
                    val throwable: Throwable
                    if (it is StatusRuntimeException) {
                        when (it.status.description) {
                            SessionsServiceErrors.USER_NOT_FOUND.description -> {
                                throwable = SessionsServiceErrors.USER_NOT_FOUND_EXCEPTION
                            }
                            else -> {
                                throwable = it
                            }
                        }
                    } else {
                        throwable = it
                    }
                    Completable.error(throwable)
                }
    }
}