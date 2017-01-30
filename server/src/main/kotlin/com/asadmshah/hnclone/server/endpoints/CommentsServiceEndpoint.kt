package com.asadmshah.hnclone.server.endpoints

import com.asadmshah.hnclone.common.tools.escape
import com.asadmshah.hnclone.errors.CommentsServiceErrors
import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.models.Comment
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.server.ServerComponent
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor
import com.asadmshah.hnclone.services.CommentCreateRequest
import com.asadmshah.hnclone.services.CommentsServiceGrpc
import io.grpc.ServerInterceptors
import io.grpc.ServerServiceDefinition
import io.grpc.stub.StreamObserver
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

    override fun create(request: CommentCreateRequest, responseObserver: StreamObserver<Comment>) {
        val session: RequestSession? = SessionInterceptor.KEY_SESSION.get()
        if (session == null) {
            responseObserver.onError(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION)
            return
        }

        val text = if (request.text.isNullOrBlank()) null else request.text.trim().escape()
        if (text == null || text.isNullOrBlank()) {
            responseObserver.onError(CommentsServiceErrors.TEXT_REQUIRED_EXCEPTION)
            return
        }

        if (text.length > 1024) {
            responseObserver.onError(CommentsServiceErrors.TEXT_TOO_LONG_EXCEPTION)
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
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        if (comment == null) {
            responseObserver.onError(CommonServiceErrors.UNKNOWN_EXCEPTION)
            return
        }

        responseObserver.onNext(comment)
        responseObserver.onCompleted()
    }

}