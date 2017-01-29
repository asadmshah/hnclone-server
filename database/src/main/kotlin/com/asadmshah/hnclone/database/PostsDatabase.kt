package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.Post
import io.reactivex.Flowable

interface PostsDatabase {

    fun create(userId: Int, title: String, text: String, url: String): Post?

    fun readTop(viewerId: Int, lim: Int = 20, off: Int = 0): Flowable<Post>

    fun readTop(viewerId: Int, userId: Int, lim: Int = 20, off: Int = 0): Flowable<Post>

    fun readNew(viewerId: Int, lim: Int = 20, off: Int = 0): Flowable<Post>

    fun readNew(viewerId: Int, userId: Int, lim: Int = 20, off: Int = 0): Flowable<Post>

    fun read(viewerId: Int, postId: Int): Post?

    fun incrementScore(viewerId: Int, postId: Int): Int?

    fun decrementScore(viewerId: Int, postId: Int): Int?

    fun removeScore(viewerId: Int, postId: Int): Int?

    fun readScore(viewerId: Int, postId: Int): Int?
}