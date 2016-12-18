package com.asadmshah.hnclone.server.interceptors

import com.asadmshah.hnclone.server.ServerComponent
import io.grpc.*
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.jsonwebtoken.Jwts

class AuthorizationInterceptor(component: ServerComponent) : ServerInterceptor {

    companion object {
        val HEADER_KEY: Metadata.Key<String> = Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER)
        val KEY_SESSION: Context.Key<Session?> = Context.key("session")

        private val STATUS_INVALID_TOKEN = Status.PERMISSION_DENIED.withDescription("Invalid Authorization Token")

        private val NOOP = object : ServerCall.Listener<Any>() {}
    }

    private val secret: String

    init {
        this.secret = component.configuration().getString("auth.secret")
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        val token = headers.get(HEADER_KEY) ?: return Contexts.interceptCall(Context.current().withValue(KEY_SESSION, null), call, headers, next)
        val context: Context
        try {
            val body = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).body
            context = Context.current().withValue(KEY_SESSION, null)
        } catch (e: Exception) {
            call.close(STATUS_INVALID_TOKEN, headers)
            return NOOP as ServerCall.Listener<ReqT>
        }

        return Contexts.interceptCall(context, call, headers, next)
    }

}