package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.User
import com.asadmshah.hnclone.services.UserCreateRequest
import com.asadmshah.hnclone.services.UserReadUsingIDRequest
import com.asadmshah.hnclone.services.UserReadUsingNameRequest
import com.asadmshah.hnclone.services.UserUpdateAboutRequest
import io.reactivex.Single

interface UsersServiceClient {

    fun create(request: UserCreateRequest): Single<User>

    fun read(request: UserReadUsingIDRequest): Single<User>

    fun read(request: UserReadUsingNameRequest): Single<User>

    fun update(request: UserUpdateAboutRequest): Single<String>

}