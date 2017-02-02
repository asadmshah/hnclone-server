package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.Comment
import io.reactivex.Flowable
import java.sql.ResultSet
import javax.inject.Inject
import javax.sql.DataSource

internal class CommentsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource) : CommentsDatabase {

    private companion object {
        // language=PostgreSQL
        const val SQL_CREATE_PARENT = "SELECT * FROM comments_create(?, ?, NULL, ?);"
        // language=PostgreSQL
        const val SQL_CREATE_CHILD = "SELECT * FROM comments_create(?, ?, ?, ?);"
        // language=PostgreSQL
        const val SQL_READ_COMMENTS_OF_POST = "SELECT * FROM comments_read_of_post(?, ?);"
        // language=PostgreSQL
        const val SQL_READ_COMMENTS_OF_COMMENT = "SELECT * FROM comments_read_of_comment(?, ?, ?);"
        // language=PostgreSQL
        const val SQL_READ_COMMENT = "SELECT * FROM comments_read_comment(?, ?, ?);"
        // language=PostgreSQL
        const val SQL_INCREMENT_SCORE = "SELECT * FROM comment_votes_upsert(?, ?, ?, 1);"
        // language=PostgreSQL
        const val SQL_DECREMENT_SCORE = "SELECT * FROM comment_votes_upsert(?, ?, ?, -1);"
        // language=PostgreSQL
        const val SQL_REMOVE_SCORE = "SELECT * FROM comment_votes_delete(?, ?, ?);"
    }

    override fun create(userId: Int, postId: Int, text: String): Comment? {
        return dataSource
                .executeSingle(SQL_CREATE_PARENT, {
                    it.setInt(1, userId)
                    it.setInt(2, postId)
                    it.setString(3, text)
                }, ResultSet::getComment)
    }

    override fun create(userId: Int, postId: Int, parentId: Int, text: String): Comment? {
        return dataSource
                .executeSingle(SQL_CREATE_CHILD, {
                    it.setInt(1, userId)
                    it.setInt(2, postId)
                    it.setInt(3, parentId)
                    it.setString(4, text)
                }, ResultSet::getComment)
    }

    override fun readComment(viewerId: Int, postId: Int, commentId: Int): Comment? {
        return dataSource
                .executeSingle(SQL_READ_COMMENT, {
                    it.setInt(1, viewerId)
                    it.setInt(2, postId)
                    it.setInt(3, commentId)
                }, ResultSet::getComment)
    }

    override fun readComments(viewerId: Int, postId: Int): Flowable<Comment> {
        return dataSource
                .executeFlowable(SQL_READ_COMMENTS_OF_POST, {
                    it.setInt(1, viewerId)
                    it.setInt(2, postId)
                }, ResultSet::getComment)
    }

    override fun readComments(viewerId: Int, postId: Int, parentId: Int): Flowable<Comment> {
        return dataSource
                .executeFlowable(SQL_READ_COMMENTS_OF_COMMENT, {
                    it.setInt(1, viewerId)
                    it.setInt(2, postId)
                    it.setInt(3, parentId)
                }, ResultSet::getComment)
    }

    override fun incrementScore(userId: Int, postId: Int, commentId: Int): Int? {
        if (readComment(userId, postId, commentId) == null) return null

        return dataSource
                .executeSingle(SQL_INCREMENT_SCORE, {
                    it.setInt(1, userId)
                    it.setInt(2, postId)
                    it.setInt(3, commentId)
                }, ResultSet::getInt)
    }

    override fun decrementScore(userId: Int, postId: Int, commentId: Int): Int? {
        if (readComment(userId, postId, commentId) == null) return null

        return dataSource
                .executeSingle(SQL_DECREMENT_SCORE, {
                    it.setInt(1, userId)
                    it.setInt(2, postId)
                    it.setInt(3, commentId)
                }, ResultSet::getInt)
    }

    override fun removeScore(userId: Int, postId: Int, commentId: Int): Int? {
        if (readComment(userId, postId, commentId) == null) return null

        return dataSource
                .executeSingle(SQL_REMOVE_SCORE, {
                    it.setInt(1, userId)
                    it.setInt(2, postId)
                    it.setInt(3, commentId)
                }, ResultSet::getInt)
    }
}