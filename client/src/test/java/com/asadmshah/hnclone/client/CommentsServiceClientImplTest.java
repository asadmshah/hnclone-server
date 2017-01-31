package com.asadmshah.hnclone.client;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.CommentsDatabase;
import com.asadmshah.hnclone.models.Comment;
import com.asadmshah.hnclone.models.CommentScore;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.models.SessionToken;
import com.asadmshah.hnclone.pubsub.PubSub;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.endpoints.CommentsServiceEndpoint;
import com.asadmshah.hnclone.services.CommentScoreResponse;
import io.reactivex.Flowable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentsServiceClientImplTest {

    @Mock private SessionManager sessionManager;
    @Mock private CommentsDatabase commentsDatabase;
    @Mock private ServerComponent component;
    @Mock private SessionStorage sessionStorage;
    @Mock private BlockedSessionsCache blockedSessionsCache;
    @Mock private PubSub pubSub;

    private BaseClient baseClient;
    private SessionsServiceClient sessionsClient;
    private CommentsServiceClient commentsClient;

    @Before
    public void setUp() throws Exception {
        when(component.commentsDatabase()).thenReturn(commentsDatabase);
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);
        when(component.pubSub()).thenReturn(pubSub);

        baseClient = TestBaseClient.create(CommentsServiceEndpoint.create(component));
        sessionsClient = new SessionsServiceClientImpl(sessionStorage, baseClient);
        commentsClient = new CommentsServiceClientImpl(sessionStorage, baseClient, sessionsClient);
    }

    @After
    public void tearDown() throws Exception {
        baseClient.shutdown();
    }

    @Test
    public void create_shouldComplete() throws Exception {
        Comment expComment = Comment.newBuilder().setId(10).build();

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(commentsDatabase.create(anyInt(), anyInt(), anyString())).thenReturn(expComment);
        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        Comment resComment = commentsClient.create(1, "Text").blockingGet();

        assertThat(resComment).isEqualTo(expComment);
    }

    @Test
    public void readComment_shouldCompleteNotLoggedIn() throws Exception {
        Comment expComment = Comment
                .newBuilder()
                .setId(10)
                .build();

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(expComment);

        Comment resComment = commentsClient.read(10, 10).blockingGet();

        assertThat(resComment).isEqualTo(expComment);

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComment(vidCaptor.capture(), anyInt(), anyInt());
        assertThat(vidCaptor.getValue()).isLessThan(1);
    }

    @Test
    public void readComment_shouldCompleteLoggedIn() throws Exception {
        Comment expComment = Comment
                .newBuilder()
                .setId(10)
                .build();

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(expComment);
        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        Comment resComment = commentsClient.read(10, 10).blockingGet();

        assertThat(resComment).isEqualTo(expComment);

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComment(vidCaptor.capture(), anyInt(), anyInt());
        assertThat(vidCaptor.getValue()).isEqualTo(requestS.getId());
    }

    @Test
    public void readCommentsFromPost_shouldCompleteNotLoggedIn() throws Exception {
        List<Comment> expComments = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            expComments.add(Comment.newBuilder().setId(i+1).build());
        }

        when(commentsDatabase.readComments(anyInt(), anyInt())).thenReturn(Flowable.fromIterable(expComments));

        List<Comment> resComments = commentsClient.readStream(10).toList().blockingGet();

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();
    }

    @Test
    public void readCommentsFromComment_shouldCompleteNotLoggedIn() throws Exception {
        List<Comment> expComments = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            expComments.add(Comment.newBuilder().setId(i+1).build());
        }

        when(commentsDatabase.readComments(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.fromIterable(expComments));

        List<Comment> resComments = commentsClient.readStream(10, 20).toList().blockingGet();

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();
    }

    @Test
    public void incrementScore_shouldComplete() throws Exception {
        CommentScoreResponse expResponse = CommentScoreResponse
                .newBuilder()
                .setPostId(1)
                .setCommentId(2)
                .setScore(3)
                .setVoted(1)
                .build();

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.incrementScore(anyInt(), anyInt())).thenReturn(3);
        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        CommentScoreResponse resResponse = commentsClient.voteIncrement(1, 2).blockingGet();

        assertThat(resResponse).isEqualTo(expResponse);
    }

    @Test
    public void decrementScore_shouldComplete() throws Exception {
        CommentScoreResponse expResponse = CommentScoreResponse
                .newBuilder()
                .setPostId(1)
                .setCommentId(2)
                .setScore(3)
                .setVoted(-1)
                .build();

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.decrementScore(anyInt(), anyInt())).thenReturn(3);
        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        CommentScoreResponse resResponse = commentsClient.voteDecrement(1, 2).blockingGet();

        assertThat(resResponse).isEqualTo(expResponse);
    }

    @Test
    public void removeScore_shouldComplete() throws Exception {
        CommentScoreResponse expResponse = CommentScoreResponse
                .newBuilder()
                .setPostId(1)
                .setCommentId(2)
                .setScore(3)
                .setVoted(0)
                .build();

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.removeScore(anyInt(), anyInt())).thenReturn(3);
        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        CommentScoreResponse resResponse = commentsClient.voteRemove(1, 2).blockingGet();

        assertThat(resResponse).isEqualTo(expResponse);
    }

    @Test
    public void subscribeComments_shouldComplete() throws Exception {
        List<Comment> comments = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            comments.add(Comment.newBuilder().setPostId(i % 2 + 1).setId(i+1).build());
        }

        when(pubSub.subComments()).thenReturn(Flowable.fromIterable(comments).delay(100, TimeUnit.MILLISECONDS));

        List<Comment> expComments = comments
                .stream()
                .filter(it -> it.getPostId() == 1)
                .limit(3)
                .collect(Collectors.toList());

        List<Comment> resComments = commentsClient
                .subscribeToCommentsStream(1)
                .take(3)
                .toList()
                .blockingGet();

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();
    }

    @Test
    public void subscribeCommentScores_shouldComplete() throws Exception {
        List<CommentScore> commentScores = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            commentScores.add(CommentScore.newBuilder().setPostId(i % 2 + 1).setCommentId(i+1).build());
        }

        when(pubSub.subCommentScores()).thenReturn(Flowable.fromIterable(commentScores).delay(100, TimeUnit.MILLISECONDS));

        List<CommentScore> expCommentScores = commentScores
                .stream()
                .filter(it -> it.getPostId() == 1)
                .limit(3)
                .collect(Collectors.toList());

        List<CommentScore> resCommentScores = commentsClient
                .subscribeToCommentScoresStream(1)
                .take(3)
                .toList()
                .blockingGet();

        assertThat(resCommentScores).containsExactlyElementsIn(expCommentScores).inOrder();
    }
}
