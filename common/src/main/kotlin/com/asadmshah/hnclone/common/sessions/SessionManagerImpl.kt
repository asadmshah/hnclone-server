package com.asadmshah.hnclone.common.sessions

import com.asadmshah.hnclone.models.Session
import com.google.protobuf.ByteString

internal class SessionManagerImpl constructor(
        private val requestKey: ByteArray,
        private val refreshKey: ByteArray,
        private val tokenizer: Tokenizer) : SessionManager {

    override fun createRequestToken(id: Int, scope: Session.Scopes, issued: Long, expire: Long): String {
        return createRequestToken(session(id, scope, issued, expire))
    }

    override fun createRequestToken(session: Session): String {
        return tokenizer.encode(requestKey, session).toStringUtf8()
    }

    override fun createRefreshToken(id: Int, scope: Session.Scopes, issued: Long, expire: Long): String {
        return createRefreshToken(session(id, scope, issued, expire))
    }

    override fun createRefreshToken(session: Session): String {
        return tokenizer.encode(refreshKey, session).toStringUtf8()
    }

    override fun parseRequestToken(token: String): Session {
        return tokenizer.decode(requestKey, ByteString.copyFromUtf8(token))
    }

    override fun parseRefreshToken(token: String): Session {
        return tokenizer.decode(refreshKey, ByteString.copyFromUtf8(token))
    }

    internal fun session(id: Int, scope: Session.Scopes, issued: Long, expire: Long): Session {
        return Session.newBuilder().setId(id).setScope(scope).setIssuedDt(issued).setExpireDt(expire).build()
    }

}