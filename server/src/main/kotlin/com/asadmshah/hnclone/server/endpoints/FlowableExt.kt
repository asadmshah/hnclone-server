package com.asadmshah.hnclone.server.endpoints

import io.grpc.stub.StreamObserver
import io.reactivex.Flowable
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

internal fun <T> Flowable<T>.blockingSubscribeStreamObserver(responseObserver: StreamObserver<T>) {
    blockingSubscribe(object : Subscriber<T> {

        private var subscription: Subscription? = null

        override fun onComplete() {
            try {
                responseObserver.onCompleted()
            } catch (ignored: Exception) {

            }
        }

        override fun onError(it: Throwable) {
            try {
                responseObserver.onError(it)
            } catch (ignored: Exception) {

            }
        }

        override fun onNext(it: T) {
            try {
                responseObserver.onNext(it)
                subscription?.request(1)
            } catch (ignored: Exception) {
                subscription?.cancel()
            }
        }

        override fun onSubscribe(it: Subscription) {
            this.subscription = it
            this.subscription?.request(1)
        }
    })
}