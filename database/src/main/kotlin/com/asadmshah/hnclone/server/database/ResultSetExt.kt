package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.Session
import com.asadmshah.hnclone.models.User
import java.sql.ResultSet

internal fun ResultSet.getSession(): Session {
    return Session(
            getInt(1),
            getString(2),
            getTimestamp(3).toLocalDateTime())
}

internal fun ResultSet.getUser(): User {
    return User(
            getInt(1),
            getString(2),
            getTimestamp(3).toLocalDateTime(),
            getInt(4),
            getString(5))
}

internal fun ResultSet.getPost(): Post {
    return Post(
            getInt(1),
            getTimestamp(2).toLocalDateTime(),
            getString(3),
            getString(4),
            getString(5),
            getInt(6),
            getInt(7),
            getString(8),
            getInt(9))
}

internal fun ResultSet.getInt(): Int {
    return getInt(1)
}

internal fun ResultSet.getBoolean(): Boolean {
    return getBoolean(1)
}

internal fun ResultSet.getString(): String? {
    return getString(1)
}