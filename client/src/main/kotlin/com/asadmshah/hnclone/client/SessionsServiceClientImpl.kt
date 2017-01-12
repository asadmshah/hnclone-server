package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.errors.SessionsServiceErrors
import com.asadmshah.hnclone.models.RefreshSession
import com.asadmshah.hnclone.services.SessionCreateRequest
import com.asadmshah.hnclone.services.SessionsServiceGrpc
import io.reactivex.Completable
import java.util.concurrent.TimeUnit

internal class
SessionsServiceClientImpl(private val sessions: SessionStorage,
                          private val baseClient: BaseClient) : SessionsServiceClient {

    companion object {
        private val CUTOFF = TimeUnit.SECONDS.toMillis(10)
    }

    override fun refresh(): Completable {
        return refresh(false)
    }

    override fun refresh(force: Boolean): Completable {
        return Completable
                .fromCallable {
                    val stub = SessionsServiceGrpc.newBlockingStub(baseClient.getChannel())
                    val rkey = sessions.getRefreshKey()
                    if (rkey != null) {
                        val tok = RefreshSession.parseFrom(rkey.data)
                        if (force || isExpired(tok.expire, TimeUnit.MILLISECONDS)) {
                            val response = stub.refresh(rkey)
                            sessions.putRequestKey(response)
                            0
                        } else {
                            0
                        }
                    } else {
                        throw SessionsServiceErrors.INVALID_TOKEN_EXCEPTION
                    }
                }
                .onStatusRuntimeErrorResumeNext()
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
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun isExpired(time: Long, unit: TimeUnit): Boolean {
        val a = unit.toMillis(time)
        val b = System.currentTimeMillis() + CUTOFF
        return a < b
    }
}