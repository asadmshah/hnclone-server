package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.tools.escape
import com.asadmshah.hnclone.common.tools.unescape
import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.errors.PostServiceErrors
import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.database.PostsDatabase
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.*
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.stub.StreamObserver
import org.apache.commons.validator.routines.UrlValidator
import rx.Subscriber
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

    init {
        this.postsDatabase = component.postsDatabase()
    }

    override fun create(request: PostCreateRequest, responseObserver: StreamObserver<Post>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val id = session.id

        val title = if (request.title.isNullOrBlank()) null else request.title.trim().escape()
        val url = if (request.url.isNullOrBlank()) null else request.url.trim().escape()
        val text = if (request.text.isNullOrBlank()) null else request.text.trim().escape()

        if (title == null) {
            responseObserver.onError(PostServiceErrors.TitleRequiredException)
            return
        }

        if (title.length > 128) {
            responseObserver.onError(PostServiceErrors.TitleTooLongException)
            return
        }

        if ((url == null || url.isBlank()) && (text == null || text.isBlank())) {
            responseObserver.onError(PostServiceErrors.ContentRequiredException)
            return
        }

        if (url != null && !urlValidator.isValid(url.unescape())) {
            responseObserver.onError(PostServiceErrors.ContentURLInvalidException)
            return
        }

        if (url != null && url.length > 128) {
            responseObserver.onError(PostServiceErrors.ContentURLUnacceptableException)
            return
        }

        if (text != null && text.length > 1024) {
            responseObserver.onError(PostServiceErrors.ContentTextTooLongException)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.create(id, title, text ?: "", url ?: "")
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (post == null) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        responseObserver.onNext(post)
        responseObserver.onCompleted()
    }

    override fun delete(request: PostDeleteRequest, responseObserver: StreamObserver<PostDeleteResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        if (post.userId != session.id) {
            responseObserver.onError(CommonServiceErrors.UnauthorizedException)
            return
        }

        val deleted: Boolean
        try {
            deleted = postsDatabase.delete(post.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        responseObserver.onNext(PostDeleteResponse.newBuilder().setId(post.id).setDeleted(deleted).build())
        responseObserver.onCompleted()
    }

    override fun read(request: PostReadRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        val post: Post?
        try {
            post = postsDatabase.read(userId, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        responseObserver.onNext(post)
        responseObserver.onCompleted()
    }

    override fun readNewStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readNew(userId, request.limit, request.offset)
                .subscribe(object : Subscriber<Post>() {
                    override fun onCompleted() {
                        responseObserver.onCompleted()
                    }

                    override fun onError(e: Throwable) {
                        when (e) {
                            is SQLException -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                            else  -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                        }
                    }

                    override fun onNext(t: Post) {
                        responseObserver.onNext(t)
                    }
                })
    }

    override fun readHotStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(userId, request.limit, request.offset)
                .subscribe(object : Subscriber<Post>() {
                    override fun onCompleted() {
                        responseObserver.onCompleted()
                    }

                    override fun onError(e: Throwable) {
                        when (e) {
                            is SQLException -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                            else  -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                        }
                    }

                    override fun onNext(t: Post) {
                        responseObserver.onNext(t)
                    }
                })
    }

    override fun readNewFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readNew(viewerId, request.id, request.limit, request.offset)
                .subscribe(object : Subscriber<Post>() {
                    override fun onCompleted() {
                        responseObserver.onCompleted();
                    }

                    override fun onError(e: Throwable) {
                        when (e) {
                            is SQLException -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                            else  -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                        }
                    }

                    override fun onNext(t: Post) {
                        responseObserver.onNext(t)
                    }
                })
    }

    override fun readTopFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(viewerId, request.id, request.limit, request.offset)
                .subscribe(object : Subscriber<Post>() {
                    override fun onCompleted() {
                        responseObserver.onCompleted()
                    }

                    override fun onError(e: Throwable) {
                        when (e) {
                            is SQLException -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                            else  -> {
                                responseObserver.onError(CommonServiceErrors.UnknownException)
                            }
                        }
                    }

                    override fun onNext(t: Post) {
                        responseObserver.onNext(t)
                    }
                })
    }

    override fun voteDecrement(request: PostVoteDecrementRequest, responseObserver: StreamObserver<PostScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.decrementScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
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
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.incrementScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
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
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.removeScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
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
}