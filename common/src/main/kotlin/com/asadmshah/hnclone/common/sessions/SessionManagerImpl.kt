package com.asadmshah.hnclone.common.sessions

import com.asadmshah.hnclone.models.RefreshSession
import com.asadmshah.hnclone.models.RequestSession
import com.asadmshah.hnclone.models.SessionScope
import com.asadmshah.hnclone.models.SessionToken
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class SessionManagerImpl constructor(req: ByteArray, ref: ByteArray) : SessionManager {

    companion object {
        private const val ALG = "HmacSHA512"

        private val REQ_EXPIRE_AFTER = TimeUnit.MINUTES.toMillis(10)
        private val REF_EXPIRE_AFTER = TimeUnit.DAYS.toMillis(90)
    }

    private val reqmac: Mac
    private val refmac: Mac

    init {
        reqmac = Mac.getInstance(ALG)
        reqmac.init(SecretKeySpec(req, ALG))
        refmac = Mac.getInstance(ALG)
        refmac.init(SecretKeySpec(ref, ALG))
    }

    override fun createRequestToken(id: Int): SessionToken {
        return createRequestToken(RequestSession
                .newBuilder()
                .setId(id)
                .setScope(SessionScope.USER)
                .setIssuedDt(System.currentTimeMillis())
                .setExpireDt(System.currentTimeMillis() + REQ_EXPIRE_AFTER)
                .build())
    }

    override fun createRequestToken(session: RequestSession): SessionToken {
        val data = session.toByteArray()
        val sign = synchronized(reqmac, { reqmac.doFinal(data) })
        return session(data, sign)
    }

    override fun parseRequestToken(token: ByteArray): RequestSession {
        return parseRequestToken(SessionToken.parseFrom(token))
    }

    override fun parseRequestToken(token: SessionToken): RequestSession {
        try {
            val data = token.data.toByteArray()
            if (!Arrays.equals(token.sign.toByteArray(), synchronized(reqmac, { reqmac.doFinal(data) }))) {
                throw TamperedTokenException()
            }

            val reqs = RequestSession.parseFrom(data)
            if (System.currentTimeMillis() > reqs.expireDt) {
                throw ExpiredTokenException()
            }

            return reqs
        } catch (e: InvalidProtocolBufferException) {
            throw InvalidTokenException(e)
        }
    }

    override fun createRefreshToken(id: Int): SessionToken {
        return createRefreshToken(RefreshSession
                .newBuilder()
                .setId(id)
                .setScope(SessionScope.USER)
                .setIssuedDt(System.currentTimeMillis())
                .setExpireDt(System.currentTimeMillis() + REF_EXPIRE_AFTER)
                .build())
    }

    override fun createRefreshToken(session: RefreshSession): SessionToken {
        val data = session.toByteArray()
        val sign = synchronized(refmac, { refmac.doFinal(data) })
        return session(data, sign)
    }

    override fun parseRefreshToken(token: ByteArray): RefreshSession {
        return parseRefreshToken(SessionToken.parseFrom(token))
    }

    override fun parseRefreshToken(token: SessionToken): RefreshSession {
        try {
            val data = token.data.toByteArray()
            if (!Arrays.equals(token.sign.toByteArray(), synchronized(refmac, { refmac.doFinal(data) }))) {
                throw TamperedTokenException()
            }

            val reqs = RefreshSession.parseFrom(data)
            if (System.currentTimeMillis() > reqs.expireDt) {
                throw ExpiredTokenException()
            }

            return reqs
        } catch (e: InvalidProtocolBufferException) {
            throw InvalidTokenException(e)
        }
    }

    internal fun session(data: ByteArray, sign: ByteArray): SessionToken {
        return SessionToken.newBuilder().setData(ByteString.copyFrom(data)).setSign(ByteString.copyFrom(sign)).build()
    }
}