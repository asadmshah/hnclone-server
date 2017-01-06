package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.RefreshSession
import com.asadmshah.hnclone.models.User
import java.sql.ResultSet
import java.time.ZoneOffset

internal fun ResultSet.getRefreshSession(): RefreshSession {
    return RefreshSession
            .newBuilder()
            .setId(getInt(1))
            .setUuid(getString(2))
            .setIssued(getTimestamp(3).toLocalDateTime().toEpochSecond(ZoneOffset.UTC))
            .build()
}

internal fun ResultSet.getUser(): User {
    return User.newBuilder()
            .setId(getInt(1))
            .setUsername(getString(2))
            .setCreated(getTimestamp(3).toLocalDateTime().toEpochSecond(ZoneOffset.UTC))
            .setScore(getInt(4))
            .setAbout(getString(5))
            .build()
}

internal fun ResultSet.getPost(): Post {
    return Post
            .newBuilder()
            .setId(getInt(1))
            .setTimestamp(getTimestamp(2).toLocalDateTime().toEpochSecond(ZoneOffset.UTC))
            .setTitle(getString(3))
            .setText(getString(4))
            .setUrl(getString(5))
            .setScore(getInt(6))
            .setUserId(getInt(7))
            .setUserName(getString(8))
            .setUpvoted(getInt(9))
            .build()
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