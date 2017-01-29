package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.Comment
import io.reactivex.Flowable
import java.sql.ResultSet
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

internal class CommentsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource) : CommentsDatabase {

    override fun create(userId: Int, postId: Int, text: String): Comment? {
        return dataSource
                .executeSingle("SELECT * FROM comments_create($userId, $postId, NULL, '$text');", ResultSet::getComment)
    }

    override fun create(userId: Int, postId: Int, parentId: Int, text: String): Comment? {
        return dataSource
                .executeSingle("SELECT * FROM comments_create($userId, $postId, $parentId, '$text');", ResultSet::getComment)
    }

    override fun readComment(viewerId: Int, postId: Int, commentId: Int): Comment? {
        try {
            return readComments(viewerId, postId, commentId).blockingFirst()
        } catch (e: NoSuchElementException) {
            return null
        }
    }

    override fun readComments(viewerId: Int, postId: Int): Flowable<Comment> {
        return dataSource
                .executeFlowable("SELECT * FROM comments_read_of_post($viewerId, $postId);", ResultSet::getComment)
    }

    override fun readComments(viewerId: Int, postId: Int, parentId: Int): Flowable<Comment> {
        return dataSource
                .executeFlowable("SELECT * FROM comments_read_of_comment($viewerId, $postId, $parentId);", ResultSet::getComment)
    }

    override fun incrementScore(userId: Int, commentId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM comment_votes_upsert($userId, $commentId, 1);", ResultSet::getInt)
    }

    override fun decrementScore(userId: Int, commentId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM comment_votes_upsert($userId, $commentId, -1);", ResultSet::getInt)
    }

    override fun removeScore(userId: Int, commentId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM comment_votes_delete($userId, $commentId);", ResultSet::getInt)
    }
}