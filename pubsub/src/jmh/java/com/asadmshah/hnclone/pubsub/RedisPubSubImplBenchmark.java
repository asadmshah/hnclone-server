package com.asadmshah.hnclone.pubsub;

import com.asadmshah.hnclone.models.Post;
import com.asadmshah.hnclone.models.PostScore;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 4)
@Measurement(iterations = 4)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class RedisPubSubImplBenchmark {

    @Param({"1", "10", "100"})
    public int publisherCount;

    @Param({"1", "10", "100"})
    public int subscriberCount;

    private PubSub pubSub;

    @Setup
    public void setUp() throws Exception {
        Configuration configuration = new Configurations().properties(RedisPubSubImplBenchmark.class.getClassLoader().getResource("test.properties"));
        this.pubSub = new RedisPubSubImpl(configuration);
        this.pubSub.start();
    }

    @TearDown
    public void tearDown() {
        this.pubSub.stop();
    }

    @Benchmark
    public void postScoreBenchmark(Blackhole blackhole) {
        for (int i = 0; i < subscriberCount; i++) {
            pubSub.subPostScore().subscribe(new PerfSubscriber(blackhole));
        }

        for (int i = 0; i < publisherCount; i++) {
            pubSub.pubPostScore(PostScore.newBuilder().setId(i).setScore(i).build());
        }
    }

    @Benchmark
    public void postBenchmark(Blackhole blackhole) {
        for (int i = 0; i < subscriberCount; i++) {
            pubSub.subPost().subscribe(new PerfSubscriber(blackhole));
        }

        for (int i = 0; i < publisherCount; i++) {
            pubSub.pubPost(Post
                    .newBuilder()
                    .setId(i)
                    .setTitle("Title " + i)
                    .setText("Text " + i)
                    .setScore(i)
                    .setUserId(i)
                    .setUserName("User " + i)
                    .build());
        }
    }

}
