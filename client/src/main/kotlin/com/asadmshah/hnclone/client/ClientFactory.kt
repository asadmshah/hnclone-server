package com.asadmshah.hnclone.client

import io.grpc.ManagedChannel

class ClientFactory(private val channel: ManagedChannel, private val sessions: SessionStorage) {

    internal fun baseClient(): BaseClient {
        return BaseClientImpl(channel)
    }

    fun commentsClient(): CommentsServiceClient {
        return CommentsServiceClientImpl(sessions, baseClient(), sessionsClient())
    }

    fun postsClient(): PostsServiceClient {
        return PostsServiceClientImpl(sessions, baseClient(), sessionsClient())
    }

    fun sessionsClient(): SessionsServiceClient {
        return SessionsServiceClientImpl(sessions, baseClient())
    }

    fun usersClient(): UsersServiceClient {
        return UsersServiceClientImpl(sessions, baseClient(), sessionsClient())
    }

}