package com.asadmshah.hnclone.pubsub;

import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;

public class PerfSubscriber implements Subscriber<Object> {

    final CountDownLatch cdl;
    final Blackhole bh;

    private Subscription subscription;

    public PerfSubscriber(Blackhole bh) {
        this.bh = bh;
        this.cdl = new CountDownLatch(1);
    }

    @Override
    public void onSubscribe(Subscription s) {
        subscription = s;

        s.request(1);
    }

    @Override
    public void onComplete() {
        subscription = null;

        cdl.countDown();
    }

    @Override
    public void onNext(Object value) {
        bh.consume(value);

        subscription.request(1);
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        subscription = null;
        cdl.countDown();
    }

}
