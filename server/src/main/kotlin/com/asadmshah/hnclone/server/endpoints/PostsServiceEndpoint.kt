package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.tools.escape
import com.asadmshah.hnclone.common.tools.unescape
import com.asadmshah.hnclone.database.PostsDatabase
import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.errors.PostServiceErrors
import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.*
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
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

    init {
        this.postsDatabase = component.postsDatabase()
    }

    override fun create(request: PostCreateRequest, responseObserver: StreamObserver<Post>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION)
            return
        }

        val id = session.id

        val title = if (request.title.isNullOrBlank()) null else request.title.trim().escape()
        val url = if (request.url.isNullOrBlank()) null else request.url.trim().escape()
        val text = if (request.text.isNullOrBlank()) null else request.text.trim().escape()

        if (title == null) {
            responseObserver.onError(PostServiceErrors.TITLE_REQUIRED_EXCEPTION)
            return
        }

        if (title.length > 128) {
            responseObserver.onError(PostServiceErrors.TITLE_TOO_LONG_EXCEPTION)
            return
        }

        if ((url == null || url.isBlank()) && (text == null || text.isBlank())) {
            responseObserver.onError(PostServiceErrors.CONTENT_REQUIRED_EXCEPTION)
            return
        }

        if (url != null && !urlValidator.isValid(url.unescape())) {
            responseObserver.onError(PostServiceErrors.CONTENT_URL_INVALID_EXCEPTION)
            return
        }

        if (url != null && url.length > 128) {
            responseObserver.onError(PostServiceErrors.CONTENT_URL_UNACCEPTABLE_EXCEPTION)
            return
        }

        if (text != null && text.length > 1024) {
            responseObserver.onError(PostServiceErrors.CONTENT_TEXT_TOO_LONG_EXCEPTION)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.create(id, title, text ?: "", url ?: "")
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        if (post == null) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        responseObserver.onNext(post)
        responseObserver.onCompleted()
    }

    override fun delete(request: PostDeleteRequest, responseObserver: StreamObserver<PostDeleteResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NOT_FOUND_EXCEPTION)
            return
        }

        if (post.userId != session.id) {
            responseObserver.onError(CommonServiceErrors.UNAUTHORIZED_EXCEPTION)
            return
        }

        val deleted: Boolean
        try {
            deleted = postsDatabase.delete(post.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
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
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NOT_FOUND_EXCEPTION)
            return
        }

        responseObserver.onNext(post)
        responseObserver.onCompleted()
    }

    override fun readNewStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readNew(userId, request.limit, request.offset)
                .doOnError {
                    when (it) {
                        is SQLException -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                        else  -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                    }
                }
                .doOnCompleted { responseObserver.onCompleted() }
                .doOnNext { responseObserver.onNext(it) }
                .doOnError {
                    if (it is StatusRuntimeException) {
                        if (it.status.code == Status.Code.CANCELLED) {
                            // Log Cancelled
                        }
                    }
                }
                .subscribe()
    }

    override fun readHotStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(userId, request.limit, request.offset)
                .doOnError {
                    when (it) {
                        is SQLException -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                        else  -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                    }
                }
                .doOnCompleted { responseObserver.onCompleted() }
                .doOnNext { responseObserver.onNext(it) }
                .doOnError {
                    if (it is StatusRuntimeException) {
                        if (it.status.code == Status.Code.CANCELLED) {
                            // Log Cancelled
                        }
                    }
                }
                .subscribe()
    }

    override fun readNewFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readNew(viewerId, request.id, request.limit, request.offset)
                .doOnError {
                    when (it) {
                        is SQLException -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                        else  -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                    }
                }
                .doOnCompleted { responseObserver.onCompleted() }
                .doOnNext { responseObserver.onNext(it) }
                .doOnError {
                    if (it is StatusRuntimeException) {
                        if (it.status.code == Status.Code.CANCELLED) {
                            // Log Cancelled
                        }
                    }
                }
                .subscribe()
    }

    override fun readTopFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        postsDatabase
                .readTop(viewerId, request.id, request.limit, request.offset)
                .doOnError {
                    when (it) {
                        is SQLException -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                        else  -> {
                            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
                        }
                    }
                }
                .doOnCompleted { responseObserver.onCompleted() }
                .doOnNext { responseObserver.onNext(it) }
                .doOnError {
                    if (it is StatusRuntimeException) {
                        if (it.status.code == Status.Code.CANCELLED) {
                            // Log Cancelled
                        }
                    }
                }
                .subscribe()
    }

    override fun voteDecrement(request: PostVoteDecrementRequest, responseObserver: StreamObserver<PostScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NOT_FOUND_EXCEPTION)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.decrementScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
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
            responseObserver.onError(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NOT_FOUND_EXCEPTION)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.incrementScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
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
            responseObserver.onError(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION)
            return
        }

        val post: Post?
        try {
            post = postsDatabase.read(session.id, request.id)
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        if (post == null) {
            responseObserver.onError(PostServiceErrors.NOT_FOUND_EXCEPTION)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.removeScore(session.id, request.id) ?: 0
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
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