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
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
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

    private val comments = component.commentsDatabase()
    private val pubsub = component.pubSub()

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
                comment = comments.create(session.id, request.postId, request.commentId, text)
            } else {
                comment = comments.create(session.id, request.postId, text)
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

        pubsub.pubComment(comment)
    }

    override fun read(request: CommentReadRequest, responseObserver: StreamObserver<Comment>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        val comment: Comment?
        try {
            comment = comments.readComment(viewerId, request.postId, request.commentId)
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

        comments
                .readComments(viewerId, request.postId)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error<Comment>(UnknownStatusException())
                }
                .subscribe(object : Subscriber<Comment> {

                    var s: Subscription? = null

                    override fun onSubscribe(s: Subscription) {
                        this.s = s
                        s.request(1)
                    }

                    override fun onNext(t: Comment) {
                        try {
                            responseObserver.onNext(t)
                            s?.request(1)
                        } catch (ignored: Exception) {
                            s?.cancel()
                        }
                    }

                    override fun onError(t: Throwable) {
                        try {
                            responseObserver.onError(t)
                        } catch (ignored: Exception) {

                        }
                    }

                    override fun onComplete() {
                        try {
                            responseObserver.onCompleted()
                        } catch (ignored: Exception) {

                        }
                    }
                })
    }

    override fun readListFromComment(request: CommentReadListFromCommentRequest, responseObserver: StreamObserver<Comment>) {
        val viewerId = SessionInterceptor.KEY_SESSION.get()?.id ?: -1

        comments
                .readComments(viewerId, request.postId, request.commentId)
                .onBackpressureBuffer()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error<Comment>(UnknownStatusException())
                }
                .subscribe(object : Subscriber<Comment> {

                    var s: Subscription? = null

                    override fun onSubscribe(s: Subscription) {
                        this.s = s
                        s.request(1)
                    }

                    override fun onNext(t: Comment) {
                        try {
                            responseObserver.onNext(t)
                            s?.request(1)
                        } catch (ignored: Exception) {
                            s?.cancel()
                        }
                    }

                    override fun onError(t: Throwable) {
                        try {
                            responseObserver.onError(t)
                        } catch (ignored: Exception) {

                        }
                    }

                    override fun onComplete() {
                        try {
                            responseObserver.onCompleted()
                        } catch (ignored: Exception) {

                        }
                    }
                })
    }

    override fun voteIncrement(request: CommentVoteIncrementRequest, responseObserver: StreamObserver<CommentScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val comment: Comment?
        try {
            comment = comments.readComment(-1, request.postId, request.commentId)
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
            score = comments.incrementScore(session.id, request.commentId)
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

        pubsub.pubCommentScore(commentScore)
    }

    override fun voteDecrement(request: CommentVoteDecrementRequest, responseObserver: StreamObserver<CommentScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val comment: Comment?
        try {
            comment = comments.readComment(-1, request.postId, request.commentId)
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
            score = comments.decrementScore(session.id, request.commentId)
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

        pubsub.pubCommentScore(commentScore)
    }

    override fun voteRemove(request: CommentVoteRemoveRequest, responseObserver: StreamObserver<CommentScoreResponse>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(UnauthenticatedStatusException())
            return
        }

        val comment: Comment?
        try {
            comment = comments.readComment(-1, request.postId, request.commentId)
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
            score = comments.removeScore(session.id, request.commentId)
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

        pubsub.pubCommentScore(commentScore)
    }

    override fun commentStream(request: CommentStreamRequest, responseObserver: StreamObserver<Comment>) {
        pubsub.subComments()
                .onBackpressureDrop()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .filter { it.postId == request.postId }
                .observeOn(Schedulers.trampoline())
                .subscribe(object : Subscriber<Comment> {

                    var s: Subscription? = null

                    override fun onSubscribe(s: Subscription) {
                        this.s = s
                        s.request(1)
                    }

                    override fun onNext(t: Comment) {
                        try {
                            responseObserver.onNext(t)
                            s?.request(1)
                        } catch (ignored: Exception) {
                            s?.cancel()
                        }
                    }

                    override fun onError(t: Throwable) {
                        try {
                            responseObserver.onError(t)
                        } catch (ignored: Exception) {

                        }
                    }

                    override fun onComplete() {
                        try {
                            responseObserver.onCompleted()
                        } catch (ignored: Exception) {

                        }
                    }
                })
    }

    override fun commentScoreStream(request: CommentScoreStreamRequest, responseObserver: StreamObserver<CommentScore>) {
        pubsub.subCommentScores()
                .onBackpressureDrop()
                .onErrorResumeNext { it: Throwable ->
                    Flowable.error(UnknownStatusException())
                }
                .filter { it.postId == request.postId }
                .observeOn(Schedulers.trampoline())
                .subscribe(object : Subscriber<CommentScore> {

                    var s: Subscription? = null

                    override fun onSubscribe(s: Subscription) {
                        this.s = s
                        s.request(1)
                    }

                    override fun onNext(t: CommentScore) {
                        try {
                            responseObserver.onNext(t)
                            s?.request(1)
                        } catch (ignored: Exception) {
                            s?.cancel()
                        }
                    }

                    override fun onError(t: Throwable) {
                        try {
                            responseObserver.onError(t)
                        } catch (ignored: Exception) {

                        }
                    }

                    override fun onComplete() {
                        try {
                            responseObserver.onCompleted()
                        } catch (ignored: Exception) {

                        }
                    }
                })
    }
}