package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import com.asadmshah.hnclone.services.*
import io.grpc.*
import io.grpc.stub.MetadataUtils
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single

internal class PostsServiceClientImpl(private val sessions: SessionStorage,
                                      private val baseClient: BaseClient,
                                      private val sessionsClient: SessionsServiceClient) : PostsServiceClient {

    override fun create(request: PostCreateRequest): Single<Post> {
        return sessionsClient
                .refresh()
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

    override fun delete(request: PostDeleteRequest): Single<Int> {
        return sessionsClient
                .refresh()
                .andThen(justDelete(request))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justDelete(request: PostDeleteRequest): Single<Int> {
        return Single
                .fromCallable {
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }
                    val stub = MetadataUtils.attachHeaders(PostsServiceGrpc.newBlockingStub(baseClient.getChannel()), md)
                    val response = stub.delete(request)

                    // TODO: Handle No Deleted Case with response.deleted.

                    response.id
                }
    }

    override fun read(request: PostReadRequest): Single<Post> {
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

    override fun readNew(request: PostReadListRequest): Flowable<Post> {
        return readStream(PostsServiceGrpc.METHOD_READ_NEW_STREAM, request, request.limit)
    }

    override fun readNew(request: PostReadListFromUserRequest): Flowable<Post> {
        return readStream(PostsServiceGrpc.METHOD_READ_NEW_FROM_USER_STREAM, request, request.limit)
    }

    override fun readHot(request: PostReadListRequest): Flowable<Post> {
        return readStream(PostsServiceGrpc.METHOD_READ_HOT_STREAM, request, request.limit)
    }

    override fun readHot(request: PostReadListFromUserRequest): Flowable<Post> {
        return readStream(PostsServiceGrpc.METHOD_READ_TOP_FROM_USER_STREAM, request, request.limit)
    }

    internal fun <T> readStream(method: MethodDescriptor<T, Post>, request: T, n: Int): Flowable<Post> {
        val f1 = sessionsClient.refresh(false, true).toFlowable<Post>()
        val f2 = Flowable.defer { justReadStream(method, request, n) }

        return Flowable.concat(f1, f2).onStatusRuntimeErrorResumeNext()
    }

    internal fun <T> justReadStream(method: MethodDescriptor<T, Post>, request: T, n: Int): Flowable<Post> {
        return Flowable
                .create<Post>({ subscriber ->
                    val md = io.grpc.Metadata()
                    sessions.getRequestKey()?.let {
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                    }

                    val call = baseClient.getChannel().newCall(method, CallOptions.DEFAULT)

                    subscriber.setCancellable { call.cancel("", null) }

                    call.start(object : ClientCall.Listener<Post>() {
                        override fun onMessage(post: Post) {
                            if (!subscriber.isCancelled) {
                                subscriber.onNext(post)
                            }
                        }

                        override fun onClose(status: Status, trailers: Metadata) {
                            if (!subscriber.isCancelled) {
                                if (!status.isOk) {
                                    subscriber.onError(StatusRuntimeException(status, trailers))
                                } else {
                                    subscriber.onComplete()
                                }
                            }
                        }

                        override fun onReady() {
                            call.sendMessage(request)
                            call.request(n)
                            call.halfClose()
                        }
                    }, md)
                }, BackpressureStrategy.BUFFER)
                .take(n.toLong())
    }

    override fun vote(request: PostVoteIncrementRequest): Single<PostScoreResponse> {
        return vote { it.voteIncrement(request) }
    }

    override fun vote(request: PostVoteDecrementRequest): Single<PostScoreResponse> {
        return vote { it.voteDecrement(request) }
    }

    override fun vote(request: PostVoteRemoveRequest): Single<PostScoreResponse> {
        return vote { it.voteRemove(request) }
    }

    internal fun vote(function: (PostsServiceGrpc.PostsServiceBlockingStub) -> PostScoreResponse): Single<PostScoreResponse> {
        return sessionsClient
                .refresh(force = false, nullable = false)
                .andThen(justVote(function))
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVote(function: (PostsServiceGrpc.PostsServiceBlockingStub) -> PostScoreResponse): Single<PostScoreResponse> {
        return Single
                .fromCallable {
                    var stub = PostsServiceGrpc.newBlockingStub(baseClient.getChannel())
                    sessions.getRequestKey()?.let {
                        val md = io.grpc.Metadata()
                        md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                        stub = MetadataUtils.attachHeaders(stub, md)
                    }
                    function(stub)
                }
    }

    override fun voteStream(): Flowable<PostScore> {
        return Flowable
                .defer { justVoteStream() }
                .onStatusRuntimeErrorResumeNext()
    }

    internal fun justVoteStream(): Flowable<PostScore> {
        return Flowable
                .create<PostScore>({ subscriber ->
                    val call = baseClient.getChannel().newCall(PostsServiceGrpc.METHOD_POST_SCORE_CHANGE_STREAM, CallOptions.DEFAULT)

                    subscriber.setCancellable { call.cancel("", null) }

                    call.start(object : ClientCall.Listener<PostScore>() {
                        override fun onMessage(message: PostScore) {
                            if (!subscriber.isCancelled) {
                                subscriber.onNext(message)
                                call.request(1)
                            }
                        }

                        override fun onClose(status: Status, trailers: Metadata) {
                            if (!subscriber.isCancelled) {
                                if (!status.isOk) {
                                    subscriber.onError(StatusRuntimeException(status, trailers))
                                } else {
                                    subscriber.onComplete()
                                }
                            }
                        }

                        override fun onReady() {
                            call.sendMessage(PostScoreChangeRequest.getDefaultInstance())
                            call.halfClose()
                            call.request(1)
                        }
                    }, Metadata())

                }, BackpressureStrategy.LATEST)
    }

}