package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Post
import rx.Observable

interface PostsDatabase {

    fun create(userId: Int, title: String, text: String, url: String): Post?

    fun readTop(viewerId: Int, lim: Int = 20, off: Int = 0): Observable<Post>

    fun readTop(viewerId: Int, userId: Int, lim: Int = 20, off: Int = 0): Observable<Post>

    fun readNew(viewerId: Int, lim: Int = 20, off: Int = 0): Observable<Post>

    fun readNew(viewerId: Int, userId: Int, lim: Int = 20, off: Int = 0): Observable<Post>

    fun read(viewerId: Int, postId: Int): Post?

    fun incrementScore(viewerId: Int, postId: Int): Int?

    fun decrementScore(viewerId: Int, postId: Int): Int?

    fun removeScore(viewerId: Int, postId: Int): Int?

    fun delete(postId: Int): Boolean
}