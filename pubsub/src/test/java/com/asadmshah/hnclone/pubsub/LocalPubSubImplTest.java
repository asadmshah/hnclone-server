package com.asadmshah.hnclone.pubsub;

import com.asadmshah.hnclone.models.Comment;
import com.asadmshah.hnclone.models.CommentScore;
import com.asadmshah.hnclone.models.Post;
import com.asadmshah.hnclone.models.PostScore;
import io.reactivex.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void pubSubComments_shouldComplete() throws Exception {
        List<Comment> expComments = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            expComments.add(Comment.newBuilder().setId(i+1).build());
        }

        CountDownLatch counter = new CountDownLatch(10);

        List<Comment> resComments = Collections.synchronizedList(new ArrayList<>(10));
        pubSub.subComments().subscribeOn(Schedulers.io()).subscribe(comment -> {
            resComments.add(comment);
            counter.countDown();
        });

        Thread.sleep(50);

        for (Comment comment : expComments) {
            pubSub.pubComment(comment);
        }

        counter.await(1, TimeUnit.SECONDS);

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();
    }

    @Test
    public void pubCommentScores_shouldComplete() throws Exception {
        List<CommentScore> expCommentScores = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            expCommentScores.add(CommentScore.newBuilder().setCommentId(i+1).build());
        }

        CountDownLatch counter = new CountDownLatch(10);

        List<CommentScore> resCommentScores = Collections.synchronizedList(new ArrayList<>(10));
        pubSub.subCommentScores().subscribeOn(Schedulers.io()).subscribe(commentScore -> {
            resCommentScores.add(commentScore);
            counter.countDown();
        });

        Thread.sleep(50);

        for (CommentScore commentScore : expCommentScores) {
            pubSub.pubCommentScore(commentScore);
        }

        counter.await(1, TimeUnit.SECONDS);

        assertThat(resCommentScores).containsExactlyElementsIn(expCommentScores).inOrder();
    }
}
