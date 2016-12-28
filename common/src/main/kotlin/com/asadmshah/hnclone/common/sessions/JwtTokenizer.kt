package com.asadmshah.hnclone.common.sessions

import com.asadmshah.hnclone.models.Session
import com.google.protobuf.ByteString
import io.jsonwebtoken.*
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

internal class JwtTokenizer @Inject constructor() : Tokenizer {

    override fun encode(key: ByteArray, session: Session): ByteString {
        val jwt = Jwts
                .builder()
                .signWith(SignatureAlgorithm.HS512, key)
                .setPayload(String(Base64.getEncoder().encode(session.toByteArray()), StandardCharsets.ISO_8859_1))
                .compact()
        return ByteString.copyFromUtf8(jwt)
    }

    override fun decode(key: ByteArray, token: ByteString): Session {
        try {
            val body = Jwts.parser().setSigningKey(key).parsePlaintextJws(token.toStringUtf8()).body
            val session = Session.parseFrom(Base64.getDecoder().decode(body.toByteArray(StandardCharsets.ISO_8859_1)))
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