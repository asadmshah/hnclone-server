package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.Comment
import io.reactivex.Flowable

interface CommentsDatabase {

    fun create(userId: Int, postId: Int, text: String): Comment?

    fun create(userId: Int, postId: Int, parentId: Int, text: String): Comment?

    fun readComment(viewerId: Int, postId: Int, commentId: Int): Comment?

    fun readComments(viewerId: Int, postId: Int): Flowable<Comment>

    fun readComments(viewerId: Int, postId: Int, parentId: Int): Flowable<Comment>

    fun incrementScore(userId: Int, postId: Int, commentId: Int): Int?

    fun decrementScore(userId: Int, postId: Int, commentId: Int): Int?

    fun removeScore(userId: Int, postId: Int, commentId: Int): Int?

}