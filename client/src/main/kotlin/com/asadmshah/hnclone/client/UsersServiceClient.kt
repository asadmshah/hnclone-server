package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.User
import com.asadmshah.hnclone.services.*
import io.reactivex.Completable
import io.reactivex.Single

interface UsersServiceClient {

    fun create(request: UserCreateRequest): Single<User>

    fun read(request: UserReadUsingIDRequest): Single<User>

    fun read(request: UserReadUsingNameRequest): Single<User>

    fun update(request: UserUpdateAboutRequest): Single<String>

    fun update(request: UserUpdatePasswordRequest): Completable

    fun delete(): Completable

}