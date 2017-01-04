package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Post
import rx.Observable
import java.sql.ResultSet
import javax.inject.Inject
import javax.sql.DataSource

internal class PostsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource): PostsDatabase {

    override fun create(userId: Int, title: String, text: String, url: String): Post? {
        return dataSource
                .executeSingle("SELECT * FROM posts_create('$title', '$text', '$url', $userId);", ResultSet::toPost)
    }

    override fun readTop(viewerId: Int, lim: Int, off: Int): Observable<Post> {
        return dataSource
                .executeObservable("SELECT * FROM posts_read_top($viewerId) LIMIT $lim OFFSET $off;", ResultSet::toPost)
    }

    override fun readTop(viewerId: Int, userId: Int, lim: Int, off: Int): Observable<Post> {
        return dataSource
                .executeObservable("SELECT * FROM posts_read_by_user_top($viewerId, $userId) LIMIT $lim OFFSET $off;", ResultSet::toPost)
    }

    override fun readNew(viewerId: Int, lim: Int, off: Int): Observable<Post> {
        return dataSource
                .executeObservable("SELECT * FROM posts_read_new($viewerId) LIMIT $lim OFFSET $off;", ResultSet::toPost)
    }

    override fun readNew(viewerId: Int, userId: Int, lim: Int, off: Int): Observable<Post> {
        return dataSource
                .executeObservable("SELECT * FROM posts_read_by_user_new($viewerId, $userId) LIMIT $lim OFFSET $off;", ResultSet::toPost)
    }

    override fun read(viewerId: Int, postId: Int): Post? {
        return dataSource
                .executeSingle("SELECT * FROM posts_read($viewerId, $postId);", ResultSet::toPost)
    }

    override fun incrementScore(viewerId: Int, postId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM post_votes_upsert($viewerId, $postId, 1);", ResultSet::toInt)
    }

    override fun decrementScore(viewerId: Int, postId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM post_votes_upsert($viewerId, $postId, -1);", ResultSet::toInt)
    }

    override fun removeScore(viewerId: Int, postId: Int): Int? {
        return dataSource
                .executeSingle("SELECT * FROM post_votes_delete($viewerId, $postId);", ResultSet::toInt)
    }

    override fun delete(postId: Int): Boolean {
        return dataSource
                .executeSingle("SELECT * FROM posts_delete($postId);", ResultSet::toBoolean) ?: false
    }
}