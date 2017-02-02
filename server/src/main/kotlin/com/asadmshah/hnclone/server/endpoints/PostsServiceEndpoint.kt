package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.tools.escape
import com.asadmshah.hnclone.common.tools.unescape
import com.asadmshah.hnclone.database.PostsDatabase
import com.asadmshah.hnclone.errors.*
import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.pubsub.PubSub
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.*
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.stub.StreamObserver
import io.reactivex.BackpressureOverflowStrategy
import io.reactivex.Flowable
import org.apache.commons.validator.routines.UrlValidator
import java.sql.SQLException

class PostsServiceEndpoint private constructor(component: ServerComponent) : PostsServiceGrpc.PostsServiceImplBase() {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): ServerServiceDefinition {
            val endpoint = PostsServiceEndpoint(component)
            val interceptor = SessionInterceptor.create(component)

            return ServerInterceptors.intercept(endpoint, interceptor)
        }

        private val urlValidator = UrlValidator()
    }

    private val postsDatabase: PostsDatabase
    private val pubSub: PubSub

    init {
        this.postsDatabase = component.postsDatabase()
        this.pubSub = component.pubSub()
    }

    override fun create(request: PostCreateRequest, responseObserver: StreamObserver<Post>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val title = if (request.title.isNullOrBlank()) null else request.title.trim().escape()
        if (title == null || title.isBlank()) {
            responseObserver.onError(PostTitleRequiredStatusException())
            return
        }

        if (title.length > 128) {
            responseObserver.onError(PostTitleTooLongStatusException())
            return
        }

        val text = if (request.text.isNullOrBlank()) null else request.text.trim().escape()
        val url = if (request.url.isNullOrBlank()) null else request.url.trim().escape()

        if (text == null && url == null) {
            responseObserver.onError(PostContentRequiredStatusException())
            return
        }

        if (text != null && text.length > 1024) {
            responseObserver.onError(PostTextTooLongStatusException())
            return
        }

        if (url != null && !urlValidator.isValid(url.unescape())) {
            responseObserver.onError(PostURLInvalidStatusException())
            return
        }

        if (url != null && url.length > 128) {
            responseObserver.onError(PostURLUnacceptableStatusException())
            return
        }

        val post: Post?
        try {
            post = postsDatabase.create(session.id, title, text ?: "", url ?: "")
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (post == null) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        responseObserver.onNext(post)
        responseObserver.onCompleted()
    }

    override fun read(request: PostReadRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        val post: Post?
        try {
            post = postsDatabase.read(userId, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (post == null) {
            responseObserver.onError(PostNotFoundStatusException())
            return
        }

        responseObserver.onNext(post)
        responseObserver.onCompleted()
    }

    override fun readNewStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readNew(userId, request.limit, request.offset)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .blockingSubscribeStreamObserver(responseObserver)
    }

    override fun readHotStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(userId, request.limit, request.offset)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .blockingSubscribeStreamObserver(responseObserver)
    }

    override fun readNewFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readNew(viewerId, request.id, request.limit, request.offset)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .blockingSubscribeStreamObserver(responseObserver)
    }

    override fun readTopFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(viewerId, request.id, request.limit, request.offset)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .blockingSubscribeStreamObserver(responseObserver)
    }

    override fun voteDecrement(request: PostVoteDecrementRequest, responseObserver: StreamObserver<PostScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (post == null) {
            responseObserver.onError(PostNotFoundStatusException())
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.decrementScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        val response = PostScoreResponse
                .newBuilder()
                .setId(request.id)
                .setScore(newScore)
                .setVoted(-1)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()

        pubSub.pubPostScore(PostScore
                .newBuilder()
                .setId(response.id)
                .setScore(response.score)
                .build())
    }

    override fun voteIncrement(request: PostVoteIncrementRequest, responseObserver: StreamObserver<PostScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (post == null) {
            responseObserver.onError(PostNotFoundStatusException())
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.incrementScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        val response = PostScoreResponse
                .newBuilder()
                .setId(request.id)
                .setScore(newScore)
                .setVoted(1)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()

        pubSub.pubPostScore(PostScore
                .newBuilder()
                .setId(response.id)
                .setScore(response.score)
                .build())
    }

    override fun voteRemove(request: PostVoteRemoveRequest, responseObserver: StreamObserver<PostScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (post == null) {
            responseObserver.onError(PostNotFoundStatusException())
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.removeScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        val response = PostScoreResponse
                .newBuilder()
                .setId(request.id)
                .setScore(newScore)
                .setVoted(0)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()

        pubSub.pubPostScore(PostScore
                .newBuilder()
                .setId(response.id)
                .setScore(response.score)
                .build())
    }

    override fun postScoreChangeStream(request: PostScoreChangeRequest, responseObserver: StreamObserver<PostScore>) {
        return pubSub
                .subPostScore()
                .onBackpressureBuffer(100, null, BackpressureOverflowStrategy.DROP_OLDEST)
                .filter { request.id <= 0 || request.id == it.id }
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .blockingSubscribeStreamObserver(responseObserver)
    }

}