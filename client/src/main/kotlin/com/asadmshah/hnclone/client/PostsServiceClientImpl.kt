package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import com.asadmshah.hnclone.services.*
import io.grpc.stub.MetadataUtils
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
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justCreate(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justCreate(request: PostCreateRequest): Single<Post> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(PostsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.create(request)
                }
    }

    override fun read(id: Int): Single<Post> {
        return read(PostReadRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun read(request: PostReadRequest): Single<Post> {
        return sessionsClient
                .refresh(force = false, nullable = true)
                .andThen(justRead(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justRead(request: PostReadRequest): Single<Post> {
        return Single
                .fromCallable {
                    var stub = PostsServiceGrpc.newBlockingStub(baseClient.getChannel())
                    sessions.getRequestKey()?.let {
                        val md = io.grpc.Metadata()
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                        stub = MetadataUtils.attachHeaders(stub, md)
                    }
                    stub.read(request)
                }
    }

    override fun readNewStream(lim: Int, off: Int): Flowable<Post> {
        return readNewStream(PostReadListRequest
                .newBuilder()
                .setLimit(lim)
                .setOffset(off)
                .build())
    }

    internal fun readNewStream(request: PostReadListRequest): Flowable<Post> {
        return sessionsClient
                .refresh(force = false, nullable = true)
                .andThen(justReadNewStream(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justReadNewStream(request: PostReadListRequest): Flowable<Post> {
        return baseClient
                .call(sessions, PostsServiceGrpc.METHOD_READ_NEW_STREAM, request, BackpressureStrategy.BUFFER)
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
        return sessionsClient
                .refresh(force = false, nullable = true)
                .andThen(justReadNewStream(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justReadNewStream(request: PostReadListFromUserRequest): Flowable<Post> {
        return baseClient
                .call(sessions, PostsServiceGrpc.METHOD_READ_NEW_FROM_USER_STREAM, request, BackpressureStrategy.BUFFER)
    }

    override fun readHotStream(lim: Int, off: Int): Flowable<Post> {
        return readHotStream(PostReadListRequest
                .newBuilder()
                .setLimit(lim)
                .setOffset(off)
                .build())
    }

    internal fun readHotStream(request: PostReadListRequest): Flowable<Post> {
        return sessionsClient
                .refresh(force = false, nullable = true)
                .andThen(justReadHotStream(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justReadHotStream(request: PostReadListRequest): Flowable<Post> {
        return baseClient
                .call(sessions, PostsServiceGrpc.METHOD_READ_HOT_STREAM, request, BackpressureStrategy.BUFFER)
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
        return sessionsClient
                .refresh(force = false, nullable = true)
                .andThen(justReadHotStream(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justReadHotStream(request: PostReadListFromUserRequest): Flowable<Post> {
        return baseClient
                .call(sessions, PostsServiceGrpc.METHOD_READ_TOP_FROM_USER_STREAM, request, BackpressureStrategy.BUFFER)
    }

    override fun voteIncrement(id: Int): Single<PostScoreResponse> {
        return voteIncrement(PostVoteIncrementRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun voteIncrement(request: PostVoteIncrementRequest): Single<PostScoreResponse> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justVoteIncrement(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVoteIncrement(request: PostVoteIncrementRequest): Single<PostScoreResponse> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(PostsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.voteIncrement(request)
                }
    }

    override fun voteDecrement(id: Int): Single<PostScoreResponse> {
        return voteDecrement(PostVoteDecrementRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun voteDecrement(request: PostVoteDecrementRequest): Single<PostScoreResponse> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justVoteDecrement(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVoteDecrement(request: PostVoteDecrementRequest): Single<PostScoreResponse> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(PostsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.voteDecrement(request)
                }
    }

    override fun voteRemove(id: Int): Single<PostScoreResponse> {
        return voteRemove(PostVoteRemoveRequest
                .newBuilder()
                .setId(id)
                .build())
    }

    internal fun voteRemove(request: PostVoteRemoveRequest): Single<PostScoreResponse> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justVoteRemove(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVoteRemove(request: PostVoteRemoveRequest): Single<PostScoreResponse> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(PostsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    stub.voteRemove(request)
                }
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
        return sessionsClient
                .refresh(force = false, nullable = true)
                .andThen(justSubscribeToPostScoresStream(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justSubscribeToPostScoresStream(request: PostScoreChangeRequest): Flowable<PostScore> {
        return baseClient
                .call(sessions, PostsServiceGrpc.METHOD_POST_SCORE_CHANGE_STREAM, request, BackpressureStrategy.LATEST)
    }

}