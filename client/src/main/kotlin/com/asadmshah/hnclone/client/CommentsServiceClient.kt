package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Comment
import com.asadmshah.hnclone.models.CommentScore
import com.asadmshah.hnclone.services.CommentScoreResponse
import io.reactivex.Flowable
import io.reactivex.Single

interface CommentsServiceClient {

    fun create(postId: Int, text: String): Single<Comment>

    fun create(postId: Int, parentId: Int, text: String): Single<Comment>

    fun read(postId: Int, commentId: Int): Single<Comment>

    fun readStream(postId: Int): Flowable<Comment>

    fun readStream(postId: Int, commentId: Int): Flowable<Comment>

    fun voteIncrement(postId: Int, commentId: Int): Single<CommentScoreResponse>

    fun voteDecrement(postId: Int, commentId: Int): Single<CommentScoreResponse>

    fun voteRemove(postId: Int, commentId: Int): Single<CommentScoreResponse>

    fun subscribeToCommentsStream(postId: Int): Flowable<Comment>

    fun subscribeToCommentScoresStream(postId: Int): Flowable<CommentScore>

}