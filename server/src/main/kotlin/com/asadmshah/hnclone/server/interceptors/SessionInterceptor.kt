package com.asadmshah.hnclone.server.interceptors

import com.asadmshah.hnclone.cache.BlockedSessionsCache
import com.asadmshah.hnclone.common.sessions.ExpiredTokenException
import com.asadmshah.hnclone.common.sessions.InvalidTokenException
import com.asadmshah.hnclone.common.sessions.SessionManager
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.server.ServerComponent
import io.grpc.*
import io.grpc.Metadata.BINARY_BYTE_MARSHALLER
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionInterceptor private constructor(component: ServerComponent) : ServerInterceptor {

    companion object {
        @JvmStatic
        fun create(component: ServerComponent): SessionInterceptor {
            return SessionInterceptor(component)
        }

        @JvmStatic val HEADER_KEY: Metadata.Key<ByteArray> = Metadata.Key.of("authorization-bin", BINARY_BYTE_MARSHALLER)
        @JvmStatic val KEY_SESSION: Context.Key<RequestSession> = Context.key("session")

        private val STATUS_INVALID_TOKEN = Status.PERMISSION_DENIED.withDescription("Invalid Request Token.")
        private val STATUS_EXPIRED_TOKEN = Status.PERMISSION_DENIED.withDescription("Expired Request Token.")

        private val NOOP = object : ServerCall.Listener<Any>() {}
    }

    private val manager: SessionManager
    private val blocked: BlockedSessionsCache

    init {
        this.manager = component.sessionManager()
        this.blocked = component.blockedSessionsCache()
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        val value = headers.get(HEADER_KEY) ?: return Contexts.interceptCall(Context.current().withValue(KEY_SESSION, null), call, headers, next)
        val context: Context
        try {
            val session = manager.parseRequestToken(value)
            val issued = LocalDateTime.ofInstant(Instant.ofEpochMilli(session.issued), ZoneOffset.UTC)
            if (blocked.contains(session.id, issued)) {
                call.close(STATUS_INVALID_TOKEN, headers)
                return NOOP as ServerCall.Listener<ReqT>
            }
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