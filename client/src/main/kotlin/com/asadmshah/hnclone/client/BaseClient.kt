package com.asadmshah.hnclone.client

import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

internal interface BaseClient {

    fun getChannel(): Channel

    fun <ReqT, ResT> call(sessions: SessionStorage, method: MethodDescriptor<ReqT, ResT>, request: ReqT, mode: BackpressureStrategy): Flowable<ResT>

    fun isShutdown(): Boolean

    fun isTerminated(): Boolean

    fun shutdown()

}