package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.services.SessionCreateRequest
import io.reactivex.Completable

interface SessionsServiceClient {

    fun refresh(): Completable

    fun refresh(force: Boolean = true): Completable

    fun create(request: SessionCreateRequest): Completable
}