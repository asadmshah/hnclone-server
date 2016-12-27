package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.tools.escape
import com.asadmshah.hnclone.common.tools.unescape
import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.errors.PostServiceErrors
import com.asadmshah.hnclone.models.*
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.database.PostsDatabase
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.PostServiceGrpc
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.stub.StreamObserver
import org.apache.commons.validator.routines.UrlValidator
import java.sql.SQLException

class PostServiceEndpoint private constructor(component: ServerComponent) : PostServiceGrpc.PostServiceImplBase() {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): ServerServiceDefinition {
            val endpoint = PostServiceEndpoint(component)
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
        val session: Session? = SessionInterceptor.KEY_SESSION.get()
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
            post = postsDatabase.create(id, title, url, text)
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
        val session: Session? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post? = postsDatabase.read(session.id, request.id)
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
            responseObserver.onError(PostServiceErrors.NotFoundException)
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

        try {
            postsDatabase.readNew(userId, request.limit, request.offset, {
                responseObserver.onNext(it)
            })
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        responseObserver.onCompleted()
    }

    override fun readHotStream(request: PostReadListRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        try {
            postsDatabase.readHot(userId, request.limit, request.offset, {
                responseObserver.onNext(it)
            })
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        responseObserver.onCompleted()
    }

    override fun readFromUserStream(request: PostReadListFromUserRequest, responseObserver: StreamObserver<Post>) {
        val userId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        try {
            postsDatabase.readFromUser(userId, request.id, request.limit, request.offset, {
                responseObserver.onNext(it)
            })
        } catch (e: SQLException) {
            responseObserver.onError(CommonServiceErrors.UnknownException)
            return
        }

        responseObserver.onCompleted()
    }

    override fun voteDecrement(request: PostVoteDecrementRequest, responseObserver: StreamObserver<PostScoreResponse>) {
        val session: Session? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post? = postsDatabase.read(session.id, request.id)
        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.voteDecrement(session.id, request.id) ?: 0
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
        val session: Session? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post? = postsDatabase.read(session.id, request.id)
        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.voteIncrement(session.id, request.id) ?: 0
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
        val session: Session? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UnauthenticatedException)
            return
        }

        val post: Post? = postsDatabase.read(session.id, request.id)
        if (post == null) {
            responseObserver.onError(PostServiceErrors.NotFoundException)
            return
        }

        val newScore: Int
        try {
            newScore = postsDatabase.voteRemove(session.id, request.id) ?: 0
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