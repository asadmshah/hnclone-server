package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.tools.escape
import com.asadmshah.hnclone.errors.*
import com.asadmshah.hnclone.models.Comment
import com.asadmshah.hnclone.models.CommentScore
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.*
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.stub.StreamObserver
import io.reactivex.BackpressureOverflowStrategy
import io.reactivex.Flowable
import java.sql.SQLException

class CommentsServiceEndpoint private constructor(component: ServerComponent) : CommentsServiceGrpc.CommentsServiceImplBase() {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): ServerServiceDefinition {
            val endpoint = CommentsServiceEndpoint(component)
            val interceptor = SessionInterceptor.create(component)

            return ServerInterceptors.intercept(endpoint, interceptor)
        }
    }

    private val commentsDatabase = component.commentsDatabase()
    private val pubSub = component.pubSub()

    override fun create(request: CommentCreateRequest, responseObserver: StreamObserver<Comment>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val text = if (request.text.isNullOrBlank()) null else request.text.trim().escape()
        if (text == null || text.isBlank()) {
            responseObserver.onError(CommentTextRequiredStatusException())
            return
        }

        if (text.length > 1024) {
            responseObserver.onError(CommentTextTooLongStatusException())
            return
        }

        val comment: Comment?
        try {
            if (request.commentId > 0) {
                comment = commentsDatabase.create(session.id, request.postId, request.commentId, text)
            } else {
                comment = commentsDatabase.create(session.id, request.postId, text)
            }
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (comment == null) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        responseObserver.onNext(comment)
        responseObserver.onCompleted()

        pubSub.pubComment(comment)
    }

    override fun read(request: CommentReadRequest, responseObserver: StreamObserver<Comment>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        val comment: Comment?
        try {
            comment = commentsDatabase.readComment(viewerId, request.postId, request.commentId)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (comment == null) {
            responseObserver.onError(CommentNotFoundStatusException())
            return
        }

        responseObserver.onNext(comment)
        responseObserver.onCompleted()
    }

    override fun readListFromPost(request: CommentReadListFromPostRequest, responseObserver: StreamObserver<Comment>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        commentsDatabase
                .readComments(viewerId, request.postId)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error<Comment>(UnknownStatusException())
                }
                .blockingSubscribeStreamObserver(responseObserver)
    }

    override fun readListFromComment(request: CommentReadListFromCommentRequest, responseObserver: StreamObserver<Comment>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        commentsDatabase
                .readComments(viewerId, request.postId, request.commentId)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error<Comment>(UnknownStatusException())
                }
                .blockingSubscribeStreamObserver(responseObserver)
    }

    override fun voteIncrement(request: CommentVoteIncrementRequest, responseObserver: StreamObserver<CommentScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val comment: Comment?
        try {
            comment = commentsDatabase.readComment(-1, request.postId, request.commentId)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (comment == null) {
            responseObserver.onError(CommentNotFoundStatusException())
            return
        }

        val score: Int?
        try {
            score = commentsDatabase.incrementScore(session.id, request.commentId)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (score == null) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        val response = CommentScoreResponse
                .newBuilder()
                .setPostId(request.postId)
                .setCommentId(request.commentId)
                .setScore(score)
                .setVoted(1)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()

        val commentScore = CommentScore
                .newBuilder()
                .setPostId(response.postId)
                .setCommentId(response.commentId)
                .setScore(response.score)
                .build()

        pubSub.pubCommentScore(commentScore)
    }

    override fun voteDecrement(request: CommentVoteDecrementRequest, responseObserver: StreamObserver<CommentScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val comment: Comment?
        try {
            comment = commentsDatabase.readComment(-1, request.postId, request.commentId)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (comment == null) {
            responseObserver.onError(CommentNotFoundStatusException())
            return
        }

        val score: Int?
        try {
            score = commentsDatabase.decrementScore(session.id, request.commentId)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (score == null) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        val response = CommentScoreResponse
                .newBuilder()
                .setPostId(request.postId)
                .setCommentId(request.commentId)
                .setScore(score)
                .setVoted(-1)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()

        val commentScore = CommentScore
                .newBuilder()
                .setPostId(response.postId)
                .setCommentId(response.commentId)
                .setScore(response.score)
                .build()

        pubSub.pubCommentScore(commentScore)
    }

    override fun voteRemove(request: CommentVoteRemoveRequest, responseObserver: StreamObserver<CommentScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val comment: Comment?
        try {
            comment = commentsDatabase.readComment(-1, request.postId, request.commentId)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (comment == null) {
            responseObserver.onError(CommentNotFoundStatusException())
            return
        }

        val score: Int?
        try {
            score = commentsDatabase.removeScore(session.id, request.commentId)
        } catch (e: SQLException) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        if (score == null) {
            responseObserver.onError(UnknownStatusException())
            return
        }

        val response = CommentScoreResponse
                .newBuilder()
                .setPostId(request.postId)
                .setCommentId(request.commentId)
                .setScore(score)
                .setVoted(0)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()

        val commentScore = CommentScore
                .newBuilder()
                .setPostId(response.postId)
                .setCommentId(response.commentId)
                .setScore(response.score)
                .build()

        pubSub.pubCommentScore(commentScore)
    }

    override fun commentStream(request: CommentStreamRequest, responseObserver: StreamObserver<Comment>) {
        pubSub.subComments()
                .onBackpressureBuffer(100, null, BackpressureOverflowStrategy.DROP_OLDEST)
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .filter { it.postId == request.postId }
                .blockingSubscribeStreamObserver(responseObserver)
    }

    override fun commentScoreStream(request: CommentScoreStreamRequest, responseObserver: StreamObserver<CommentScore>) {
        pubSub.subCommentScores()
                .onBackpressureBuffer(100, null, BackpressureOverflowStrategy.DROP_OLDEST)
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .filter { it.postId == request.postId }
                .blockingSubscribeStreamObserver(responseObserver)
    }
}