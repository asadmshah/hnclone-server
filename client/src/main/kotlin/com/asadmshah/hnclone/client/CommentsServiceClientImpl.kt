package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Comment
import com.asadmshah.hnclone.models.CommentScore
import com.asadmshah.hnclone.services.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single

internal class CommentsServiceClientImpl(private val sessions: SessionStorage,
                                         private val baseClient: BaseClient,
                                         private val sessionsClient: SessionsServiceClient) : CommentsServiceClient {

    override fun create(postId: Int, text: String): Single<Comment> {
        return create(CommentCreateRequest
                .newBuilder()
                .setPostId(postId)
                .setText(text)
                .build())
    }

    override fun create(postId: Int, parentId: Int, text: String): Single<Comment> {
        return create(CommentCreateRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(parentId)
                .setText(text)
                .build())
    }

    internal fun create(request: CommentCreateRequest): Single<Comment> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<Comment>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_CREATE, request, BackpressureStrategy.BUFFER)

        return Flowable.concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
                .firstOrError()
    }

    override fun read(postId: Int, commentId: Int): Single<Comment> {
        return read(CommentReadRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun read(request: CommentReadRequest): Single<Comment> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Comment>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_READ, request, BackpressureStrategy.BUFFER)

        return Flowable.concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
                .firstOrError()
    }

    override fun readStream(postId: Int): Flowable<Comment> {
        return readStream(CommentReadListFromPostRequest
                .newBuilder()
                .setPostId(postId)
                .build())
    }

    internal fun readStream(request: CommentReadListFromPostRequest): Flowable<Comment> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Comment>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_READ_LIST_FROM_POST, request, BackpressureStrategy.BUFFER)

        return Flowable.concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun readStream(postId: Int, commentId: Int): Flowable<Comment> {
        return readStream(CommentReadListFromCommentRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun readStream(request: CommentReadListFromCommentRequest): Flowable<Comment> {
        val f1 = sessionsClient.refresh(false, true).toFlowable<Comment>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_READ_LIST_FROM_COMMENT, request, BackpressureStrategy.BUFFER)

        return Flowable.concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun voteIncrement(postId: Int, commentId: Int): Single<CommentScoreResponse> {
        return voteIncrement(CommentVoteIncrementRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun voteIncrement(request: CommentVoteIncrementRequest): Single<CommentScoreResponse> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<CommentScoreResponse>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_VOTE_INCREMENT, request, BackpressureStrategy.BUFFER)

        return Flowable.concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun voteDecrement(postId: Int, commentId: Int): Single<CommentScoreResponse> {
        return voteDecrement(CommentVoteDecrementRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun voteDecrement(request: CommentVoteDecrementRequest): Single<CommentScoreResponse> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<CommentScoreResponse>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_VOTE_DECREMENT, request, BackpressureStrategy.BUFFER)

        return Flowable.concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun voteRemove(postId: Int, commentId: Int): Single<CommentScoreResponse> {
        return voteRemove(CommentVoteRemoveRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun voteRemove(request: CommentVoteRemoveRequest): Single<CommentScoreResponse> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<CommentScoreResponse>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_VOTE_REMOVE, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun subscribeToCommentsStream(postId: Int): Flowable<Comment> {
        return subscribeToCommentsStream(CommentStreamRequest
                .newBuilder()
                .setPostId(postId)
                .build())
    }

    internal fun subscribeToCommentsStream(request: CommentStreamRequest): Flowable<Comment> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Comment>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_COMMENT_STREAM, request, BackpressureStrategy.LATEST)

        return Flowable
                .concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun subscribeToCommentScoresStream(postId: Int): Flowable<CommentScore> {
        return subscribeToCommentScoresStream(CommentScoreStreamRequest
                .newBuilder()
                .setPostId(postId)
                .build())
    }

    internal fun subscribeToCommentScoresStream(request: CommentScoreStreamRequest): Flowable<CommentScore> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<CommentScore>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_COMMENT_SCORE_STREAM, request, BackpressureStrategy.LATEST)

        return Flowable
                .concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }
}