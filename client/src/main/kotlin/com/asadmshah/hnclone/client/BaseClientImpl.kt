package com.asadmshah.hnclone.client

import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

internal class BaseClientImpl(host: String, port: Int, plaintext: Boolean = true) : BaseClient {

    private val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext(plaintext)
            .build()

    override fun getChannel(): Channel {
        return channel
    }

    override fun isShutdown(): Boolean {
        return channel.isShutdown
    }

    override fun isTerminated(): Boolean {
        return channel.isTerminated
    }

    override fun shutdown() {
        channel.shutdownNow()
    }
}