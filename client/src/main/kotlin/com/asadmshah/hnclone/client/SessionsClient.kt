package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.services.SessionCreateRequest
import io.reactivex.Completable

interface SessionsClient {

    fun refresh(): Completable

    fun create(request: SessionCreateRequest): Completable
}