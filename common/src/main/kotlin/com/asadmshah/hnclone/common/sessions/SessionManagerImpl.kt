package com.asadmshah.hnclone.common.sessions

import com.google.protobuf.AbstractMessage
import io.jsonwebtoken.*
import org.apache.commons.configuration2.Configuration
import java.nio.charset.StandardCharsets
import java.util.*

internal class SessionManagerImpl private constructor(private val requestKey: ByteArray, private val refreshKey: ByteArray) : SessionManager {

    internal companion object {
        @JvmStatic
        fun create(req: ByteArray, ref: ByteArray): SessionManagerImpl {
            return SessionManagerImpl(req, ref)
        }

        @JvmStatic
        fun create(configuration: Configuration): SessionManagerImpl {
            val req = configuration.getString("auth.secret.request").toByteArray()
            val ref = configuration.getString("auth.secret.refresh").toByteArray()
            return create(req, ref)
        }
    }

    override fun createRequestToken(id: Int, scope: Session.Scopes, issued: Long, expire: Long): String {
        return createRequestToken(session(id, scope, issued, expire))
    }

    override fun createRequestToken(session: Session): String {
        return Jwts
                .builder()
                .signWith(SignatureAlgorithm.HS512, requestKey)
                .setPayload(encode(session))
                .compact()
    }

    override fun createRefreshToken(id: Int, scope: Session.Scopes, issued: Long, expire: Long): String {
        return createRefreshToken(session(id, scope, issued, expire))
    }

    override fun createRefreshToken(session: Session): String {
        return Jwts
                .builder()
                .signWith(SignatureAlgorithm.HS512, refreshKey)
                .setPayload(encode(session))
                .compact()
    }

    override fun parseRequestToken(token: String): Session {
        return parse(requestKey, token)
    }

    override fun parseRefreshToken(token: String): Session {
        return parse(refreshKey, token)
    }

    internal fun encode(message: AbstractMessage): String {
        return String(Base64.getEncoder().encode(message.toByteArray()), StandardCharsets.ISO_8859_1)
    }

    internal fun decode(message: String): ByteArray {
        return Base64.getDecoder().decode(message.toByteArray(StandardCharsets.ISO_8859_1))
    }

    internal fun session(id: Int, scope: Session.Scopes, issued: Long, expire: Long): Session {
        return Session.newBuilder().setId(id).setScope(scope).setIssuedDt(issued).setExpireDt(expire).build()
    }

    internal fun parse(key: ByteArray, jws: String): Session {
        try {
            val body = Jwts.parser().setSigningKey(key).parsePlaintextJws(jws).body
            val session = Session.parseFrom(decode(body))
            if (session.expireDt <= System.currentTimeMillis()) {
                throw ExpiredTokenException(session)
            }
            return session
        } catch (e: UnsupportedJwtException) {
            throw InvalidTokenException(e.message, e.cause)
        } catch (e: MalformedJwtException) {
            throw InvalidTokenException(e.message, e.cause)
        } catch (e: SignatureException) {
            throw InvalidTokenException(e.message, e.cause)
        } catch (e: IllegalArgumentException) {
            throw InvalidTokenException(e.message, e.cause)
        }
    }
}