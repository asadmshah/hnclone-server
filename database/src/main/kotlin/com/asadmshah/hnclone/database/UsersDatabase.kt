package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.User

interface UsersDatabase {

    fun create(username: String, password: String, about: String = ""): User?

    fun read(id: Int): User?

    fun read(username: String): User?

    fun read(username: String, password: String): User?

    fun updateAbout(id: Int, about: String = ""): String?

    fun updatePassword(id: Int, password: String): Boolean?

    fun delete(id: Int): Boolean?
}
