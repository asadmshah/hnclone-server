package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.Post
import io.reactivex.Flowable
import java.sql.ResultSet
import javax.inject.Inject
import javax.sql.DataSource

internal class PostsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource): PostsDatabase {

    private companion object {
        // language=PostgreSQL
        const val SQL_CREATE = "SELECT * FROM posts_create(?, ?, ?, ?);"
        // language=PostgreSQL
        const val SQL_READ_TOP = "SELECT * FROM posts_read_top(?) LIMIT ? OFFSET ?;"
        // language=PostgreSQL
        const val SQL_READ_TOP_FROM_USER = "SELECT * FROM posts_read_by_user_top(?, ?) LIMIT ? OFFSET ?;"
        // language=PostgreSQL
        const val SQL_READ_NEW = "SELECT * FROM posts_read_new(?) LIMIT ? OFFSET ?;"
        // language=PostgreSQL
        const val SQL_READ_NEW_FROM_USER = "SELECT * FROM posts_read_by_user_new(?, ?) LIMIT ? OFFSET ?;"
        // language=PostgreSQL
        const val SQL_READ_POST = "SELECT * FROM posts_read(?, ?);"
        // language=PostgreSQL
        const val SQL_INCREMENT_SCORE = "SELECT * FROM post_votes_upsert(?, ?, 1);"
        // language=PostgreSQL
        const val SQL_DECREMENT_SCORE = "SELECT * FROM post_votes_upsert(?, ?, -1);"
        // language=PostgreSQL
        const val SQL_REMOVE_SCORE = "SELECT * FROM post_votes_delete(?, ?);"
    }

    override fun create(userId: Int, title: String, text: String, url: String): Post? {
        return dataSource.executeSingle(SQL_CREATE, {
            it.setString(1, title)
            it.setString(2, text)
            it.setString(3, url)
            it.setInt(4, userId)
        }, ResultSet::getPost)
    }

    override fun readTop(viewerId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource.executeFlowable(SQL_READ_TOP, {
            it.setInt(1, viewerId)
            it.setInt(2, lim)
            it.setInt(3, off)
        }, ResultSet::getPost)
    }

    override fun readTop(viewerId: Int, userId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource.executeFlowable(SQL_READ_TOP_FROM_USER, {
            it.setInt(1, viewerId)
            it.setInt(2, userId)
            it.setInt(3, lim)
            it.setInt(4, off)
        }, ResultSet::getPost)
    }

    override fun readNew(viewerId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource.executeFlowable(SQL_READ_NEW, {
            it.setInt(1, viewerId)
            it.setInt(2, lim)
            it.setInt(3, off)
        }, ResultSet::getPost)
    }

    override fun readNew(viewerId: Int, userId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource.executeFlowable(SQL_READ_NEW_FROM_USER, {
            it.setInt(1, viewerId)
            it.setInt(2, userId)
            it.setInt(3, lim)
            it.setInt(4, off)
        }, ResultSet::getPost)
    }

    override fun read(viewerId: Int, postId: Int): Post? {
        return dataSource.executeSingle(SQL_READ_POST, {
            it.setInt(1, viewerId)
            it.setInt(2, postId)
        }, ResultSet::getPost)
    }

    override fun incrementScore(viewerId: Int, postId: Int): Int? {
        if (read(0, postId) == null) return null

        return dataSource.executeSingle(SQL_INCREMENT_SCORE, {
            it.setInt(1, viewerId)
            it.setInt(2, postId)
        }, ResultSet::getInt)
    }

    override fun decrementScore(viewerId: Int, postId: Int): Int? {
        if (read(0, postId) == null) return null

        return dataSource.executeSingle(SQL_DECREMENT_SCORE, {
            it.setInt(1, viewerId)
            it.setInt(2, postId)
        }, ResultSet::getInt)
    }

    override fun removeScore(viewerId: Int, postId: Int): Int? {
        if (read(0, postId) == null) return null

        return dataSource.executeSingle(SQL_REMOVE_SCORE, {
            it.setInt(1, viewerId)
            it.setInt(2, postId)
        }, ResultSet::getInt)
    }
}