package com.asadmshah.hnclone.client;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.PostsDatabase;
import com.asadmshah.hnclone.database.SessionsDatabase;
import com.asadmshah.hnclone.errors.SessionsServiceErrors;
import com.asadmshah.hnclone.models.Post;
import com.asadmshah.hnclone.models.PostScore;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.models.SessionToken;
import com.asadmshah.hnclone.pubsub.PubSub;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.endpoints.PostsServiceEndpoint;
import com.asadmshah.hnclone.services.*;
import com.google.common.collect.Lists;
import io.grpc.StatusRuntimeException;
import io.reactivex.Flowable;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostsServiceClientImplTest {

    @Mock private SessionManager sessionManager;
    @Mock private PostsDatabase postsDatabase;
    @Mock private SessionsDatabase sessionsDatabase;
    @Mock private ServerComponent component;
    @Mock private SessionStorage sessionStorage;
    @Mock private BlockedSessionsCache blockedSessionsCache;
    @Mock private PubSub pubSub;

    private BaseClient baseClient;
    private SessionsServiceClient sessionsClient;
    private PostsServiceClientImpl postsServiceClient;

    @Before
    public void setUp() throws Exception {
        when(component.postsDatabase()).thenReturn(postsDatabase);
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);
        when(component.pubSub()).thenReturn(pubSub);

        baseClient = new TestBaseClientImpl(PostsServiceEndpoint.create(component));
        sessionsClient = new SessionsServiceClientImpl(sessionStorage, baseClient);
        postsServiceClient = new PostsServiceClientImpl(sessionStorage, baseClient, sessionsClient);
    }

    @After
    public void tearDown() throws Exception {
        baseClient.shutdown();
    }

    @Test
    public void create_ShouldComplete() throws Exception {
        PostCreateRequest request = PostCreateRequest
                .newBuilder()
                .setTitle("Title")
                .setText("Text")
                .setUrl("http://www.google.com")
                .build();

        Post expPost = Post
                .newBuilder()
                .setTitle(request.getTitle())
                .setText(request.getText())
                .setUrl(request.getUrl())
                .build();

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(postsDatabase.create(anyInt(), anyString(), anyString(), anyString())).thenReturn(expPost);
        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        Post post = postsServiceClient.create(request).blockingGet();

        String expUrl = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(request.getUrl()));

        verify(postsDatabase).create(10, request.getTitle(), request.getText(), expUrl);

        assertThat(post).isNotNull();
        assertThat(post.getTitle()).isEqualTo(request.getTitle());
        assertThat(post.getText()).isEqualTo(request.getText());
        assertThat(post.getUrl()).isEqualTo(request.getUrl());
    }

    @Test
    public void delete_shouldComplete() throws Exception {
        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.newBuilder().setId(20).setUserId(10).build());
        when(postsDatabase.delete(anyInt())).thenReturn(true);

        PostDeleteRequest request = PostDeleteRequest
                .newBuilder()
                .setId(20)
                .build();

        int response = postsServiceClient.delete(request).blockingGet();

        assertThat(response).isEqualTo(20);

        verify(postsDatabase).read(10, 20);
        verify(postsDatabase).delete(20);
    }

    @Test
    public void delete_shouldThrowInvalidTokenException() throws Exception {
        PostDeleteRequest request = PostDeleteRequest
                .newBuilder()
                .setId(20)
                .build();

        StatusRuntimeException exception = null;
        try {
            postsServiceClient.delete(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus()).isEqualTo(SessionsServiceErrors.INVALID_TOKEN);
    }

    @Test
    public void readSinglePost_shouldCompleteNotLoggedIn() throws Exception {
        Post expPost = Post
                .newBuilder()
                .setId(20)
                .build();

        PostReadRequest request = PostReadRequest
                .newBuilder()
                .setId(expPost.getId())
                .build();

        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(expPost);

        Post resPost = postsServiceClient.read(request).blockingGet();

        assertThat(resPost).isEqualTo(expPost);

        ArgumentCaptor<Integer> uidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(postsDatabase).read(uidCaptor.capture(), pidCaptor.capture());
        assertThat(uidCaptor.getValue()).isLessThan(0);
        assertThat(pidCaptor.getValue()).isEqualTo(expPost.getId());
    }

    @Test
    public void readSinglePost_shouldCompleteLoggedIn() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        Post expPost = Post
                .newBuilder()
                .setId(20)
                .build();

        PostReadRequest request = PostReadRequest
                .newBuilder()
                .setId(expPost.getId())
                .build();

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(expPost);

        Post resPost = postsServiceClient.read(request).blockingGet();

        assertThat(resPost).isEqualTo(expPost);

        verify(postsDatabase).read(requestSession.getId(), request.getId());
    }

    @Test
    public void readNewPosts_shouldCompleteNotLoggedIn() throws Exception {
        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readNew(request).blockingIterable(5));

        assertThat(rposts).hasSize(5);
        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void readNewPosts_shouldUnsubscribeEarlier() throws Exception {
        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readNew(request).take(3).blockingIterable(5));

        assertThat(rposts).hasSize(3);
        assertThat(rposts).containsExactly(epost1, epost2, epost3);
    }

    @Test
    public void readNewPosts_shouldReturnLessThanRequested() throws Exception {
        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();

        List<Post> eposts = Arrays.asList(epost1, epost2);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readNew(request).blockingIterable(5));

        assertThat(rposts).hasSize(2);
        assertThat(rposts).containsExactly(epost1, epost2);
    }

    @Test
    public void readNewPosts_shouldCompleteLoggedIn() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readNew(request).blockingIterable());

        assertThat(rposts).hasSize(5);
        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void readHotPosts_shouldCompleteNotLoggedIn() throws Exception {
        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readHot(request).blockingIterable(5));

        assertThat(rposts).hasSize(5);
        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void readHotPosts_shouldCompleteLoggedIn() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readHot(request).blockingIterable());

        assertThat(rposts).hasSize(5);
        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void readNewPostsFromUser_shouldCompleteNotLoggedIn() throws Exception {
        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(50)
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readNew(request).blockingIterable());

        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void readNewPostsFromUser_shouldCompleteLoggedIn() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(50)
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readNew(request).blockingIterable());

        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void readTopPostsFromUser_shouldCompleteNotLoggedIn() throws Exception {
        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(50)
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readHot(request).blockingIterable());

        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void readTopPostsFromUser_shouldCompleteLoggedIn() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        Post epost1 = Post.newBuilder().setId(1).build();
        Post epost2 = Post.newBuilder().setId(2).build();
        Post epost3 = Post.newBuilder().setId(3).build();
        Post epost4 = Post.newBuilder().setId(4).build();
        Post epost5 = Post.newBuilder().setId(5).build();

        List<Post> eposts = Arrays.asList(epost1, epost2, epost3, epost4, epost5);

        Flowable<Post> fposts = Flowable.fromIterable(eposts);

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(fposts);

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(50)
                .setLimit(5)
                .setOffset(0)
                .build();

        List<Post> rposts = Lists.newArrayList(postsServiceClient.readHot(request).blockingIterable());

        assertThat(rposts).containsExactlyElementsIn(eposts);
    }

    @Test
    public void voteIncrement_shouldComplete() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.incrementScore(anyInt(), anyInt())).thenReturn(2);

        PostVoteIncrementRequest request = PostVoteIncrementRequest
                .newBuilder()
                .setId(12)
                .build();

        postsServiceClient.vote(request).blockingGet();

        verify(postsDatabase).incrementScore(requestSession.getId(), request.getId());
    }

    @Test
    public void voteIncrement_shouldFailOnNoSession() throws Exception {
        when(sessionStorage.getRequestKey()).thenReturn(null);

        PostVoteIncrementRequest request = PostVoteIncrementRequest
                .newBuilder()
                .setId(12)
                .build();

        StatusRuntimeException exception = null;
        try {
            postsServiceClient.vote(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();

        verifyNoMoreInteractions(sessionManager);
        verifyNoMoreInteractions(postsDatabase);
    }

    @Test
    public void voteDecrement_shouldComplete() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.decrementScore(anyInt(), anyInt())).thenReturn(2);

        PostVoteDecrementRequest request = PostVoteDecrementRequest
                .newBuilder()
                .setId(12)
                .build();

        postsServiceClient.vote(request).blockingGet();

        verify(postsDatabase).decrementScore(requestSession.getId(), request.getId());
    }

    @Test
    public void voteRemove_shouldComplete() throws Exception {
        RequestSession requestSession = RequestSession
                .newBuilder()
                .setId(10)
                .setExpire(System.currentTimeMillis() + 60_000)
                .build();

        SessionToken sessionToken = SessionToken
                .newBuilder()
                .setData(requestSession.toByteString())
                .build();

        when(sessionStorage.getRequestKey()).thenReturn(sessionToken);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestSession);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.removeScore(anyInt(), anyInt())).thenReturn(2);

        PostVoteRemoveRequest request = PostVoteRemoveRequest
                .newBuilder()
                .setId(12)
                .build();

        postsServiceClient.vote(request).blockingGet();

        verify(postsDatabase).removeScore(requestSession.getId(), request.getId());
    }

    @Test
    public void postVoteStream_shouldComplete() throws Exception {
        List<PostScore> scores = new ArrayList<>(2);
        for (int i = 0; i < 10; i++) {
            scores.add(PostScore.newBuilder().setId(i).build());
        }

        List<PostScore> expScores = scores.subList(0, 5);

        when(pubSub.subPostScore()).thenReturn(Flowable.fromIterable(scores).delay(16, TimeUnit.MILLISECONDS));

        List<PostScore> resScores = Lists.newArrayList(postsServiceClient.voteStream().take(5).blockingIterable());

        assertThat(resScores).containsExactlyElementsIn(expScores);
    }

    @Test
    public void postVoteStreamFiltered_shouldComplete() throws Exception {
        List<PostScore> scores = new ArrayList<>(2);
        for (int i = 0; i < 10; i++) {
            scores.add(PostScore.newBuilder().setId(i).build());
        }

        PostScore expScore = scores.get(5);

        when(pubSub.subPostScore()).thenReturn(Flowable.fromIterable(scores).delay(16, TimeUnit.MILLISECONDS));

        PostScore resScore = postsServiceClient.voteStream(expScore.getId()).blockingFirst();

        assertThat(resScore).isEqualTo(expScore);
    }

}
