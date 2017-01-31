package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Comment
import com.asadmshah.hnclone.models.CommentScore
import com.asadmshah.hnclone.services.*
import io.grpc.stub.MetadataUtils
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
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justCreate(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justCreate(request: CommentCreateRequest): Single<Comment> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(CommentsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.create(request)
                }
    }

    override fun read(postId: Int, commentId: Int): Single<Comment> {
        return read(CommentReadRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun read(request: CommentReadRequest): Single<Comment> {
        return sessionsClient
                .refresh(force = false, nullable = true)
                .andThen(justRead(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justRead(request: CommentReadRequest): Single<Comment> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(CommentsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.read(request)
                }
    }

    override fun readStream(postId: Int): Flowable<Comment> {
        return readStream(CommentReadListFromPostRequest
                .newBuilder()
                .setPostId(postId)
                .build())
    }

    internal fun readStream(request: CommentReadListFromPostRequest): Flowable<Comment> {
        val f1 = sessionsClient.refresh(false, true).toFlowable<Comment>()
        val f2 = baseClient.call(sessions, CommentsServiceGrpc.METHOD_READ_LIST_FROM_POST, request, BackpressureStrategy.BUFFER)

        return Flowable.concat(f1, f2).onStatusRuntimeErrorResumeNext()
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

        return Flowable.concat(f1, f2).onStatusRuntimeErrorResumeNext()
    }

    override fun voteIncrement(postId: Int, commentId: Int): Single<CommentScoreResponse> {
        return voteIncrement(CommentVoteIncrementRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun voteIncrement(request: CommentVoteIncrementRequest): Single<CommentScoreResponse> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justVote(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVote(request: CommentVoteIncrementRequest): Single<CommentScoreResponse> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(CommentsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.voteIncrement(request)
                }
    }

    override fun voteDecrement(postId: Int, commentId: Int): Single<CommentScoreResponse> {
        return voteDecrement(CommentVoteDecrementRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun voteDecrement(request: CommentVoteDecrementRequest): Single<CommentScoreResponse> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justVote(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVote(request: CommentVoteDecrementRequest): Single<CommentScoreResponse> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(CommentsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.voteDecrement(request)
                }
    }

    override fun voteRemove(postId: Int, commentId: Int): Single<CommentScoreResponse> {
        return voteRemove(CommentVoteRemoveRequest
                .newBuilder()
                .setPostId(postId)
                .setCommentId(commentId)
                .build())
    }

    internal fun voteRemove(request: CommentVoteRemoveRequest): Single<CommentScoreResponse> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justVote(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVote(request: CommentVoteRemoveRequest): Single<CommentScoreResponse> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(CommentsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.voteRemove(request)
                }
    }

    override fun subscribeToCommentsStream(postId: Int): Flowable<Comment> {
        return subscribeToCommentsStream(CommentStreamRequest
                .newBuilder()
                .setPostId(postId)
                .build())
    }

    internal fun subscribeToCommentsStream(request: CommentStreamRequest): Flowable<Comment> {
        return baseClient
                .call(sessions, CommentsServiceGrpc.METHOD_COMMENT_STREAM, request, BackpressureStrategy.DROP)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun subscribeToCommentScoresStream(postId: Int): Flowable<CommentScore> {
        return subscribeToCommentScoresStream(CommentScoreStreamRequest
                .newBuilder()
                .setPostId(postId)
                .build())
    }

    internal fun subscribeToCommentScoresStream(request: CommentScoreStreamRequest): Flowable<CommentScore> {
        return baseClient
                .call(sessions, CommentsServiceGrpc.METHOD_COMMENT_SCORE_STREAM, request, BackpressureStrategy.DROP)
                .onStatusRuntimeErrorResumeNext()
    }
}