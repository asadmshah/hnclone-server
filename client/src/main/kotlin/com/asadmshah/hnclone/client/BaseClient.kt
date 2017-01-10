package com.asadmshah.hnclone.client

import io.grpc.Channel

internal interface BaseClient {

    fun getChannel(): Channel

    fun isShutdown(): Boolean

    fun isTerminated(): Boolean

    fun shutdown()

}