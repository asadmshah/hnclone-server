package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.errors.ServiceError
import io.grpc.StatusRuntimeException
import io.reactivex.*
import io.reactivex.functions.Function
import org.reactivestreams.Publisher

internal fun Completable.onStatusRuntimeErrorResumeNext(): Completable {
    return onErrorResumeNext {
        if (it is StatusRuntimeException) {
            Completable.error(ServiceError.restore(it))
        } else {
            Completable.error(it)
        }
    }
}

internal fun <T> Single<T>.onStatusRuntimeErrorResumeNext(): Single<T> {
    return onErrorResumeNext {
        if (it is StatusRuntimeException) {
            Single.error(ServiceError.restore(it))
        } else {
            Single.error(it)
        }
    }
}

internal fun <T> Observable<T>.onStatusRuntimeErrorResumeNext(): Observable<T> {
    return onErrorResumeNext(Function<Throwable, ObservableSource<T>> {
        if (it is StatusRuntimeException) {
            Observable.error(ServiceError.restore(it))
        } else {
            Observable.error(it)
        }
    })
}

internal fun <T> Flowable<T>.onStatusRuntimeErrorResumeNext(): Flowable<T> {
    return onErrorResumeNext(Function<Throwable, Publisher<T>> {
        if (it is StatusRuntimeException) {
            Flowable.error(ServiceError.restore(it))
        } else {
            Flowable.error(it)
        }
    })
}