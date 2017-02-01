package com.asadmshah.hnclone.client

import io.reactivex.Completable

interface SessionsServiceClient {

    fun refresh(force: Boolean = true, nullable: Boolean = false): Completable

    fun create(username: String, password: String): Completable
}