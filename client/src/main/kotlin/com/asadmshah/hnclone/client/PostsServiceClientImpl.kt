package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import com.asadmshah.hnclone.services.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single

internal class PostsServiceClientImpl(private val sessions: SessionStorage,
                                      private val baseClient: BaseClient,
                                      private val sessionsClient: SessionsServiceClient) : PostsServiceClient {

    override fun create(title: String, text: String?, url: String?): Single<Post> {
        return create(PostCreateRequest
                .newBuilder()
                .setTitle(title)
                .setText(text ?: "")
                .setUrl(url ?: "")
                .build())
    }

    internal fun create(request: PostCreateRequest): Single<Post> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<Post>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_CREATE, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun read(id: Int): Single<Post> {
        return read(PostReadRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun read(request: PostReadRequest): Single<Post> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Post>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_READ, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun readNewStream(lim: Int, off: Int): Flowable<Post> {
        return readNewStream(PostReadListRequest
                .newBuilder()
                .setLimit(lim)
                .setOffset(off)
                .build())
    }

    internal fun readNewStream(request: PostReadListRequest): Flowable<Post> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Post>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_READ_NEW_STREAM, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun readNewStream(userId: Int, lim: Int, off: Int): Flowable<Post> {
        return readNewStream(PostReadListFromUserRequest
                .newBuilder()
                .setId(userId)
                .setLimit(lim)
                .setOffset(off)
                .build())
    }

    internal fun readNewStream(request: PostReadListFromUserRequest): Flowable<Post> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Post>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_READ_NEW_FROM_USER_STREAM, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun readHotStream(lim: Int, off: Int): Flowable<Post> {
        return readHotStream(PostReadListRequest
                .newBuilder()
                .setLimit(lim)
                .setOffset(off)
                .build())
    }

    internal fun readHotStream(request: PostReadListRequest): Flowable<Post> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Post>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_READ_HOT_STREAM, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun readHotStream(userId: Int, lim: Int, off: Int): Flowable<Post> {
        return readHotStream(PostReadListFromUserRequest
                .newBuilder()
                .setId(userId)
                .setLimit(lim)
                .setOffset(off)
                .build())
    }

    internal fun readHotStream(request: PostReadListFromUserRequest): Flowable<Post> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<Post>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_READ_TOP_FROM_USER_STREAM, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

    override fun voteIncrement(id: Int): Single<PostScoreResponse> {
        return voteIncrement(PostVoteIncrementRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun voteIncrement(request: PostVoteIncrementRequest): Single<PostScoreResponse> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<PostScoreResponse>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_VOTE_INCREMENT, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun voteDecrement(id: Int): Single<PostScoreResponse> {
        return voteDecrement(PostVoteDecrementRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun voteDecrement(request: PostVoteDecrementRequest): Single<PostScoreResponse> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<PostScoreResponse>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_VOTE_DECREMENT, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun voteRemove(id: Int): Single<PostScoreResponse> {
        return voteRemove(PostVoteRemoveRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun voteRemove(request: PostVoteRemoveRequest): Single<PostScoreResponse> {
        val f1 = sessionsClient.refresh(force = false, nullable = false).toFlowable<PostScoreResponse>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_VOTE_REMOVE, request, BackpressureStrategy.BUFFER)

        return Flowable
                .concat(f1, f2)
                .firstOrError()
                .onStatusRuntimeErrorResumeNext()
    }

    override fun subscribeToPostScoresStream(): Flowable<PostScore> {
        return subscribeToPostScoresStream(PostScoreChangeRequest.getDefaultInstance())
    }

    override fun subscribeToPostScoresStream(id: Int): Flowable<PostScore> {
        return subscribeToPostScoresStream(PostScoreChangeRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun subscribeToPostScoresStream(request: PostScoreChangeRequest): Flowable<PostScore> {
        val f1 = sessionsClient.refresh(force = false, nullable = true).toFlowable<PostScore>()
        val f2 = baseClient.call(sessions, PostsServiceGrpc.METHOD_POST_SCORE_CHANGE_STREAM, request, BackpressureStrategy.LATEST)

        return Flowable.concat(f1, f2)
                .onStatusRuntimeErrorResumeNext()
    }

}