package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.Post
import io.reactivex.Flowable
import java.sql.ResultSet
import javax.inject.Inject
import javax.sql.DataSource

internal class PostsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource): PostsDatabase {

    override fun create(userId: Int, title: String, text: String, url: String): Post? {
        return dataSource
                .executeSingle("SELECT * FROM posts_create('$title', '$text', '$url', $userId);", ResultSet::getPost)
    }

    override fun readTop(viewerId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource
                .executeFlowable("SELECT * FROM posts_read_top($viewerId) LIMIT $lim OFFSET $off;", ResultSet::getPost)
    }

    override fun readTop(viewerId: Int, userId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource
                .executeFlowable("SELECT * FROM posts_read_by_user_top($viewerId, $userId) LIMIT $lim OFFSET $off;", ResultSet::getPost)
    }

    override fun readNew(viewerId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource
                .executeFlowable("SELECT * FROM posts_read_new($viewerId) LIMIT $lim OFFSET $off;", ResultSet::getPost)
    }

    override fun readNew(viewerId: Int, userId: Int, lim: Int, off: Int): Flowable<Post> {
        return dataSource
                .executeFlowable("SELECT * FROM posts_read_by_user_new($viewerId, $userId) LIMIT $lim OFFSET $off;", ResultSet::getPost)
    }

    override fun read(viewerId: Int, postId: Int): Post? {
        return dataSource
                .executeSingle("SELECT * FROM posts_read($viewerId, $postId);", ResultSet::getPost)
    }

    override fun incrementScore(viewerId: Int, postId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM post_votes_upsert($viewerId, $postId, 1);", ResultSet::getInt)
    }

    override fun decrementScore(viewerId: Int, postId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM post_votes_upsert($viewerId, $postId, -1);", ResultSet::getInt)
    }

    override fun removeScore(viewerId: Int, postId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM post_votes_delete($viewerId, $postId);", ResultSet::getInt)
    }

    override fun readScore(viewerId: Int, postId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM post_votes_read_score($viewerId, $postId);", ResultSet::getInt)
    }
}