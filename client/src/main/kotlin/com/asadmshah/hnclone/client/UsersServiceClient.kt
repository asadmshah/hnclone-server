package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.User
import io.reactivex.Completable
import io.reactivex.Single

interface UsersServiceClient {

    fun create(username: String, password: String, about: String): Single<User>

    fun read(id: Int): Single<User>

    fun updateAbout(about: String): Single<String>

    fun updatePassword(password: String): Completable
}