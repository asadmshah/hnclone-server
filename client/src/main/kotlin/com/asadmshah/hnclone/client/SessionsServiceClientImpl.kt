package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.errors.SessionInvalidTokenStatusException
import com.asadmshah.hnclone.models.RequestSession
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

    override fun refresh(force: Boolean, nullable: Boolean): Completable {
        return Completable
                .fromCallable {
                    val reqk = sessions.getRequestKey()
                    if (reqk != null) {
                        val reqt = RequestSession.parseFrom(reqk.data)
                        if (force || isExpired(reqt.expire, TimeUnit.MILLISECONDS)) {
                            val stub = SessionsServiceGrpc.newBlockingStub(baseClient.getChannel())
                            val refk = sessions.getRefreshKey()
                            val response = stub.refresh(refk)
                            sessions.putRequestKey(response)
                            0
                        } else {
                            0
                        }
                    } else {
                        if (nullable) {
                            0
                        } else {
                            throw SessionInvalidTokenStatusException()
                        }
                    }
                }
                .onStatusRuntimeErrorResumeNext()
    }

    override fun create(username: String, password: String): Completable {
        return create(SessionCreateRequest
                .newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build())
    }

    internal fun create(request: SessionCreateRequest): Completable {
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