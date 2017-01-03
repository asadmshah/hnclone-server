package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Session
import com.asadmshah.hnclone.models.User
import java.sql.ResultSet

internal fun ResultSet.toSession(): Session {
    return Session(
            getInt(1),
            getString(2),
            getTimestamp(3).toLocalDateTime())
}

internal fun ResultSet.toUser(): User {
    return User(
            getInt(1),
            getString(2),
            getTimestamp(3).toLocalDateTime(),
            getInt(4),
            getString(5))
}

internal fun ResultSet.toInt(): Int {
    return getInt(1)
}

internal fun ResultSet.toBoolean(): Boolean {
    return getBoolean(1)
}

internal fun ResultSet.getString(): String? {
    return getString(1)
}