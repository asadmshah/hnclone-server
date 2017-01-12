package com.asadmshah.hnclone.client

import io.grpc.StatusRuntimeException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.functions.Function

internal fun Completable.onStatusRuntimeErrorResumeNext(): Completable {
    return onErrorResumeNext {
        if (it is StatusRuntimeException) {
            Completable.error(restoreError(it))
        } else {
            Completable.error(it)
        }
    }
}

internal fun <T> Single<T>.onStatusRuntimeErrorResumeNext(): Single<T> {
    return onErrorResumeNext {
        if (it is StatusRuntimeException) {
            Single.error(restoreError(it))
        } else {
            Single.error(it)
        }
    }
}

internal fun <T> Observable<T>.onStatusRuntimeErrorResumeNext(): Observable<T> {
    return onErrorResumeNext(Function<Throwable, ObservableSource<T>> {
        if (it is StatusRuntimeException) {
            Observable.error(restoreError(it))
        } else {
            Observable.error(it)
        }
    })
}