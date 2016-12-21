package com.asadmshah.hnclone.server.interceptors

import com.asadmshah.hnclone.common.sessions.ExpiredTokenException
import com.asadmshah.hnclone.common.sessions.InvalidTokenException
import com.asadmshah.hnclone.common.sessions.SessionManager
import com.asadmshah.hnclone.models.Session
import com.asadmshah.hnclone.server.ServerComponent
import io.grpc.*
import io.grpc.Metadata.ASCII_STRING_MARSHALLER

class SessionInterceptor private constructor(private val manager: SessionManager) : ServerInterceptor {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): SessionInterceptor {
            return create(component.sessionManager())
        }

        @JvmStatic
        fun create(sessionManager: SessionManager): SessionInterceptor {
            return SessionInterceptor(sessionManager)
        }

        @JvmStatic val HEADER_KEY: Metadata.Key<String> = Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER)
        @JvmStatic val KEY_SESSION: Context.Key<Session?> = Context.key("session")

        private val STATUS_INVALID_TOKEN = Status.PERMISSION_DENIED.withDescription("Invalid Request Token.")
        private val STATUS_EXPIRED_TOKEN = Status.PERMISSION_DENIED.withDescription("Expired Request Token.")

        private val BEARER = "Bearer "

        private val NOOP = object : ServerCall.Listener<Any>() {}
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        val value = headers.get(HEADER_KEY) ?: return Contexts.interceptCall(Context.current().withValue(KEY_SESSION, null), call, headers, next)
        val token = if (value.startsWith(BEARER)) value.substring(BEARER.length) else ""
        val context: Context
        try {
            val session = manager.parseRequestToken(token)
            context = Context.current().withValue(KEY_SESSION, session)
        } catch (e: ExpiredTokenException) {
            call.close(STATUS_EXPIRED_TOKEN, headers)
            return NOOP as ServerCall.Listener<ReqT>
        } catch (e: InvalidTokenException) {
            call.close(STATUS_INVALID_TOKEN, headers)
            return NOOP as ServerCall.Listener<ReqT>
        }

        return Contexts.interceptCall(context, call, headers, next)
    }

}