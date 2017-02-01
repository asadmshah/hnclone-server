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
import org.apache.commons.validator.routines.UrlValidator
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
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

        val id = session.id

        val title = if (request.title.isNullOrBlank()) null else request.title.trim().escape()
        val url = if (request.url.isNullOrBlank()) null else request.url.trim().escape()
        val text = if (request.text.isNullOrBlank()) null else request.text.trim().escape()

        if (title == null) {
            responseObserver.onError(PostTitleRequiredStatusException())
            return
        }

        if (title.length > 128) {
            responseObserver.onError(PostTitleTooLongStatusException())
            return
        }

        if ((url == null || url.isBlank()) && (text == null || text.isBlank())) {
            responseObserver.onError(PostContentRequiredStatusException())
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

        if (text != null && text.length > 1024) {
            responseObserver.onError(PostTextTooLongStatusException())
            return
        }

        val post: Post?
        try {
            post = postsDatabase.create(id, title, text ?: "", url ?: "")
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
                .subscribe(object : Subscriber<Post> {

                    private var subscription: Subscription? = null

                    override fun onComplete() {
                        responseObserver.onCompleted()
                    }

                    override fun onNext(it: Post) {
                        responseObserver.onNext(it)
                        subscription?.request(1)
                    }

                    override fun onError(it: Throwable) {
                        when (it) {
                            is SQLException -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                            else -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                        }
                    }

                    override fun onSubscribe(s: Subscription) {
                        subscription = s
                        s.request(1)
                    }
                })
    }

    override fun readHotStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(userId, request.limit, request.offset)
                .subscribe(object : Subscriber<Post> {

                    private var subscription: Subscription? = null

                    override fun onComplete() {
                        responseObserver.onCompleted()
                    }

                    override fun onNext(it: Post) {
                        responseObserver.onNext(it)
                        subscription?.request(1)
                    }

                    override fun onError(it: Throwable) {
                        when (it) {
                            is SQLException -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                            else -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                        }
                    }

                    override fun onSubscribe(s: Subscription) {
                        subscription = s
                        s.request(1)
                    }
                })
    }

    override fun readNewFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readNew(viewerId, request.id, request.limit, request.offset)
                .subscribe(object : Subscriber<Post> {

                    private var subscription: Subscription? = null

                    override fun onComplete() {
                        responseObserver.onCompleted()
                    }

                    override fun onNext(it: Post) {
                        responseObserver.onNext(it)
                        subscription?.request(1)
                    }

                    override fun onError(it: Throwable) {
                        when (it) {
                            is SQLException -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                            else -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                        }
                    }

                    override fun onSubscribe(s: Subscription) {
                        subscription = s
                        s.request(1)
                    }
                })
    }

    override fun readTopFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(viewerId, request.id, request.limit, request.offset)
                .subscribe(object : Subscriber<Post> {

                    private var subscription: Subscription? = null

                    override fun onComplete() {
                        responseObserver.onCompleted()
                    }

                    override fun onNext(it: Post) {
                        responseObserver.onNext(it)
                        subscription?.request(1)
                    }

                    override fun onError(it: Throwable) {
                        when (it) {
                            is SQLException -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                            else -> {
                                responseObserver.onError(UnknownStatusException())
                            }
                        }
                    }

                    override fun onSubscribe(s: Subscription) {
                        subscription = s
                        s.request(1)
                    }
                })
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
    }

    override fun postScoreChangeStream(request: PostScoreChangeRequest, responseObserver: StreamObserver<PostScore>) {
        var flowable = pubSub.subPostScore()
        if (request.id > 0) {
            flowable = flowable.filter { it.id == request.id }
        }

        flowable
                .onBackpressureLatest()
                .subscribe(object : Subscriber<PostScore> {

                    private var s: Subscription? = null

                    override fun onSubscribe(s: Subscription) {
                        this.s = s
                        this.s?.request(1)
                    }

                    override fun onError(t: Throwable) {
                        try {
                            responseObserver.onError(UnknownStatusException())
                        } catch (e: Exception) {

                        }
                    }

                    override fun onNext(v: PostScore) {
                        try {
                            responseObserver.onNext(v)
                            this.s?.request(1)
                        } catch (e: Exception) {

                        }
                    }

                    override fun onComplete() {
                        try {
                            responseObserver.onCompleted()
                        } catch (e: Exception) {

                        }
                    }
                })
    }

}