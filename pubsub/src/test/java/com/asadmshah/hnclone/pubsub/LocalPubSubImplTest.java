package com.asadmshah.hnclone.pubsub;

import com.asadmshah.hnclone.models.Post;
import com.asadmshah.hnclone.models.PostScore;
import io.reactivex.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class LocalPubSubImplTest {

    private PubSub pubSub;

    @Before
    public void setUp() throws Exception {
        pubSub = new LocalPubSubImpl();
        pubSub.start();
    }

    @After
    public void tearDown() throws Exception {
        pubSub.stop();
    }

    @Test
    public void pubSubPostScore_shouldComplete() throws Exception {
        PostScore postScore1 = PostScore.newBuilder().setId(1).setScore(10).build();
        PostScore postScore2 = PostScore.newBuilder().setId(2).setScore(20).build();

        List<PostScore> results1 = Collections.synchronizedList(new ArrayList<>());
        List<PostScore> results2 = Collections.synchronizedList(new ArrayList<>());

        pubSub.subPostScore().subscribeOn(Schedulers.io()).subscribe(results1::add);
        Thread.sleep(50);
        pubSub.pubPostScore(postScore1);
        pubSub.subPostScore().subscribeOn(Schedulers.io()).subscribe(results2::add);
        Thread.sleep(50);
        pubSub.pubPostScore(postScore2);

        assertThat(results1).containsExactly(postScore1, postScore2);
        assertThat(results2).containsExactly(postScore2);
    }

    @Test
    public void pubSubPost_shouldComplete() throws Exception {
        Post post1 = Post.newBuilder().setId(1).setScore(10).build();
        Post post2 = Post.newBuilder().setId(2).setScore(20).build();

        List<Post> results1 = Collections.synchronizedList(new ArrayList<>());
        List<Post> results2 = Collections.synchronizedList(new ArrayList<>());

        pubSub.subPost().subscribeOn(Schedulers.io()).subscribe(results1::add);
        Thread.sleep(50);
        pubSub.pubPost(post1);
        pubSub.subPost().subscribeOn(Schedulers.io()).subscribe(results2::add);
        Thread.sleep(50);
        pubSub.pubPost(post2);

        assertThat(results1).containsExactly(post1, post2);
        assertThat(results2).containsExactly(post2);
    }
}
