package com.asadmshah.hnclone.common.sessions

import com.asadmshah.hnclone.models.Session
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

// TODO: Test Thoroughly.
class SessionManagerImplTest {

    private val requestKey: String = "request"
    private val refreshKey: String = "refresh"

    private var sessionManager: SessionManager? = null

    @Before
    fun setUp() {
        sessionManager = SessionManagerImpl(requestKey.toByteArray(), refreshKey.toByteArray(), JwtTokenizer())
    }

    @After
    fun tearDown() {

    }

    @Test
    fun testCreateRequestTokenFromValues() {
        val exp = createSession()
        val res = sessionManager!!.parseRequestToken(sessionManager!!.createRequestToken(exp.id, exp.scope, exp.issuedDt, exp.expireDt))

        assertThat(res).isEqualTo(exp)
    }

    @Test
    fun testCreateRequestTokenShouldUseCorrectIssuedDateTime() {
        val dta = System.currentTimeMillis()
        Thread.sleep(10)
        val dtb = sessionManager!!.parseRequestToken(sessionManager!!.createRequestToken(1)).issuedDt

        assertThat(dtb).isGreaterThan(dta)
    }

    @Test
    fun testCreateRequestTokenFromSession() {
        val exp = createSession()
        val res = sessionManager!!.parseRequestToken(sessionManager!!.createRequestToken(exp))

        assertThat(res).isEqualTo(exp)
    }

    @Test
    fun testCreateRefreshTokenFromSessionCorrect() {
        val exp = createSession()
        val res = sessionManager!!.parseRefreshToken(sessionManager!!.createRefreshToken(exp))

        assertThat(res).isEqualTo(exp)
    }

    private fun createSession(id: Int = 1, idt: Long = System.currentTimeMillis(), edt: Long = System.currentTimeMillis() * 10000): Session {
        return Session
                .newBuilder()
                .setId(id)
                .setScope(Session.Scopes.USER)
                .setIssuedDt(idt)
                .setExpireDt(edt)
                .build()
    }

}