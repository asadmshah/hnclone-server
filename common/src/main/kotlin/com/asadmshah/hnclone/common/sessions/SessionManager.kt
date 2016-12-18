package com.asadmshah.hnclone.common.sessions

import java.util.concurrent.TimeUnit

/**
 * Helps manage [Session] through the use of request/refresh tokens.
 */
interface SessionManager {

    /**
     * Creates a request token by generating a [Session] using the given arguments.
     *
     * @param id of the token.
     * @param scope of the token. Defaults to [Session.Scopes.USER].
     * @param issued datetime in milliseconds. Defaults to current time.
     * @param expire datetime in milliseconds. Defaults to current time + 10 minutes.
     *
     * @return a request token to be used with API calls.
     */
    fun createRequestToken(id: Int, scope: Session.Scopes = Session.Scopes.USER, issued: Long = System.currentTimeMillis(), expire: Long = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)): String

    /**
     * Creates a request token using the given [Session].
     *
     * @param session to base the token off of.
     *
     * @return a request token to be used with API calls.
     */
    fun createRequestToken(session: Session): String

    /**
     * Creates a refresh token by generating a [Session] using the given arguments.
     *
     * @param id of the token.
     * @param scope of the token. Defaults to [Session.Scopes.USER].
     * @param issued datetime in milliseconds. Defaults to current time.
     * @param expire datetime in milliseconds. Defaults to current time + 90 days.
     *
     * @return a refresh token to be used with API calls.
     */
    fun createRefreshToken(id: Int, scope: Session.Scopes = Session.Scopes.USER, issued: Long = System.currentTimeMillis(), expire: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(90)): String

    /**
     * Creates a refresh token using the given [Session].
     *
     * @param session to base the token off of.
     *
     * @return a refresh token to be used with API calls.
     */
    fun createRefreshToken(session: Session): String

    /**
     * Generates a [Session] using the given request token.
     *
     * @param token request string.
     *
     * @throws ExpiredTokenException
     * @throws InvalidTokenException
     *
     * @return a [Session].
     */
    fun parseRequestToken(token: String): Session

    /**
     * Generates a [Session] using the given refresh token.
     *
     * @param token refresh string.
     *
     * @throws ExpiredTokenException
     * @throws InvalidTokenException
     *
     * @return a [Session].
     */
    fun parseRefreshToken(token: String): Session
}