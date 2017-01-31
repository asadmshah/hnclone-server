package com.asadmshah.hnclone.client

import io.grpc.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

internal open class BaseClientImpl internal constructor(private val channel: ManagedChannel): BaseClient {

    override fun getChannel(): Channel {
        return channel
    }

    override fun <ReqT, ResT> call(sessions: SessionStorage, method: MethodDescriptor<ReqT, ResT>, request: ReqT, mode: BackpressureStrategy): Flowable<ResT> {
        return Flowable.defer {
            Flowable.create<ResT>({ subscriber ->
                val md = io.grpc.Metadata()
                sessions.getRequestKey()?.let {
                    md.put(Constants.AUTHORIZATION_KEY, it.toByteArray())
                }

                val call = channel.newCall(method, CallOptions.DEFAULT)

                subscriber.setCancellable { call.cancel("Cancelled.", null) }

                call.start(object : ClientCall.Listener<ResT>() {
                    override fun onClose(status: Status, trailers: Metadata) {
                        if (!subscriber.isCancelled) {
                            if (!status.isOk) {
                                subscriber.onError(StatusRuntimeException(status, trailers))
                            } else {
                                subscriber.onComplete()
                            }
                        }
                    }

                    override fun onMessage(message: ResT) {
                        if (!subscriber.isCancelled) {
                            subscriber.onNext(message)
                        }
                    }

                    override fun onReady() {
                        call.sendMessage(request)
                        call.request(Integer.MAX_VALUE)
                        call.halfClose()
                    }
                }, md)
            }, mode)
        }
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