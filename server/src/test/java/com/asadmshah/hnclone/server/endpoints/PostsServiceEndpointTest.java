package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.common.tools.StringExtKt;
import com.asadmshah.hnclone.database.PostsDatabase;
import com.asadmshah.hnclone.errors.*;
import com.asadmshah.hnclone.models.Post;
import com.asadmshah.hnclone.models.PostScore;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.pubsub.PubSub;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor;
import com.asadmshah.hnclone.services.*;
import com.asadmshah.hnclone.services.PostsServiceGrpc.PostsServiceBlockingStub;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.reactivex.Flowable;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostsServiceEndpointTest {

    private static final String SERVER_NAME = "in-process server for " + PostsServiceEndpoint.class.getSimpleName();

    private ManagedChannel inProcessChannel;
    private Server inProcessServer;

    private PostsServiceBlockingStub inProcessStub;

    @Mock private SessionManager sessionManager;
    @Mock private PostsDatabase postsDatabase;
    @Mock private ServerComponent component;
    @Mock private BlockedSessionsCache blockedSessionsCache;
    @Mock private PubSub pubSub;

    @Captor private ArgumentCaptor<Integer> uidCaptor;
    @Captor private ArgumentCaptor<String> pscTitleCaptor;
    @Captor private ArgumentCaptor<String> pscUrlCaptor;
    @Captor private ArgumentCaptor<String> pscTextCaptor;
    @Captor private ArgumentCaptor<Integer> psdIdCaptor;
    @Captor private ArgumentCaptor<Integer> psrIdCaptor;
    @Captor private ArgumentCaptor<Integer> psvdIdCaptor;
    @Captor private ArgumentCaptor<Integer> psviIdCaptor;
    @Captor private ArgumentCaptor<Integer> psvrIdCaptor;

    private RequestSession testSession;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.postsDatabase()).thenReturn(postsDatabase);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);
        when(component.pubSub()).thenReturn(pubSub);

        when(blockedSessionsCache.contains(anyInt(), any(LocalDateTime.class))).thenReturn(false);

        inProcessChannel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        inProcessServer = InProcessServerBuilder
                .forName(SERVER_NAME)
                .addService(PostsServiceEndpoint.create(component))
                .directExecutor()
                .build();

        inProcessServer.start();

        inProcessStub = PostsServiceGrpc.newBlockingStub(inProcessChannel);

        testSession = RequestSession
                .newBuilder()
                .setId(10)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        inProcessChannel.shutdownNow();
        inProcessServer.shutdownNow();
    }

    @Test
    public void createPost_shouldComplete() {
        Post expPost = Post.getDefaultInstance();

        when(sessionManager.parseRequestToken(any(byte[].class)))
                .thenReturn(testSession);
        when(postsDatabase.create(anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(expPost);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("http://www.test.com")
                .setText("Test Text")
                .build();

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Post resPost = inProcessStub.create(req);

        verify(postsDatabase).create(uidCaptor.capture(), pscTitleCaptor.capture(), pscTextCaptor.capture(), pscUrlCaptor.capture());

        assertThat(resPost).isNotNull();
        assertThat(resPost).isEqualTo(expPost);

        assertThat(uidCaptor.getValue()).isEqualTo(testSession.getId());
        assertThat(pscTitleCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getTitle()));
        assertThat(pscTextCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getText()));
        assertThat(pscUrlCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getUrl()));
    }

    @Test
    public void createPost_shouldThrowErrorOnNoSession() {
        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("http://www.test.com")
                .setText("Test Text")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnauthenticatedStatusException.class);
    }

    @Test
    public void createPost_shouldThrowErrorOnEmptyTitle() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("")
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostTitleRequiredStatusException.class);
    }

    @Test
    public void createPost_shouldThrowErrorOnNullTitle() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostTitleRequiredStatusException.class);
    }


    @Test
    public void createPost_shouldThrowErrorOnLongTitle() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle(StringUtils.repeat("T", 129))
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostTitleTooLongStatusException.class);
    }

    @Test
    public void createPost_shouldThrowErrorOnNoContent() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostContentRequiredStatusException.class);
    }
    @Test
    public void createPost_shouldSucceedWithTitleAndUrlButNoText() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.create(anyInt(), anyString(), anyString(), anyString())).thenReturn(Post.getDefaultInstance());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("http://www.google.com")
                .build();

        Post resPost = inProcessStub.create(req);

        verify(postsDatabase).create(uidCaptor.capture(), pscTitleCaptor.capture(), pscTextCaptor.capture(), pscUrlCaptor.capture());

        assertThat(resPost).isNotNull();
        assertThat(resPost).isEqualTo(Post.getDefaultInstance());

        assertThat(uidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(pscTitleCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getTitle()));
        assertThat(pscTextCaptor.getValue()).isEmpty();
        assertThat(pscUrlCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getUrl()));
    }

    @Test
    public void createPost_shouldSucceedWithTitleAndTextButNoUrl() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.create(anyInt(), anyString(), anyString(), anyString())).thenReturn(Post.getDefaultInstance());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setText("Test Text")
                .build();

        Post resPost = inProcessStub.create(req);

        verify(postsDatabase).create(uidCaptor.capture(), pscTitleCaptor.capture(), pscTextCaptor.capture(), pscUrlCaptor.capture());

        assertThat(resPost).isNotNull();
        assertThat(resPost).isEqualTo(Post.getDefaultInstance());

        assertThat(uidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(pscTitleCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getTitle()));
        assertThat(pscTextCaptor.getValue()).isEqualTo(req.getText());
        assertThat(pscUrlCaptor.getValue()).isEmpty();
    }

    @Test
    public void createPost_shouldThrowErrorOnInvalidURL() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("htpt://www.google.com")
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostURLInvalidStatusException.class);
    }

    @Test
    public void createPost_shouldThrowErrorTextTooLong() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setText(StringUtils.repeat("T", 1025))
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostTextTooLongStatusException.class);
    }

    @Test
    public void createPost_shouldThrowErrorOnDatabaseError() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("http://www.test.com")
                .setText("Test Text")
                .build();

        when(postsDatabase.create(anyInt(), anyString(), anyString(), anyString()))
                .thenThrow(SQLException.class);

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void createPost_shouldThrowErrorOnCreateReturningNull() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("http://www.test.com/")
                .setText("Test Text")
                .build();

        when(postsDatabase.create(anyInt(), anyString(), anyString(), anyString())).thenReturn(null);

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void createPost_shouldThrowErrorOnLongUrl() {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("http://www.test.com/" + StringUtils.repeat("T", 128))
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostURLUnacceptableStatusException.class);
    }

    @Test
    public void readPost_shouldCompleteWithNoSession() {
        Post exp = Post
                .newBuilder()
                .setId(10)
                .build();

        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(exp);

        PostReadRequest req = PostReadRequest
                .newBuilder()
                .setId(exp.getId())
                .build();

        Post res = inProcessStub.read(req);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());

        assertThat(uidCaptor.getValue()).isLessThan(1);
        assertThat(psrIdCaptor.getValue()).isEqualTo(exp.getId());

        assertThat(res).isNotNull();
        assertThat(res).isEqualTo(exp);
    }

    @Test
    public void readPost_shouldCompleteWithSession() {
        RequestSession session = RequestSession.newBuilder().setId(100).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Post exp = Post
                .newBuilder()
                .setId(10)
                .build();

        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(exp);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostReadRequest req = PostReadRequest
                .newBuilder()
                .setId(exp.getId())
                .build();

        Post res = inProcessStub.read(req);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());

        assertThat(uidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(psrIdCaptor.getValue()).isEqualTo(exp.getId());

        assertThat(res).isNotNull();
        assertThat(res).isEqualTo(exp);
    }

    @Test
    public void readPost_shouldThrowPostNotFoundError() {
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        PostReadRequest req = PostReadRequest
                .newBuilder()
                .setId(1)
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.read(req);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostNotFoundStatusException.class);
    }

    @Test
    public void readPost_shouldThrowSQLException() {
        when(postsDatabase.read(anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.read(PostReadRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void voteDecrement_shouldThrowUnauthenticatedError() {
        PostVoteDecrementRequest request = PostVoteDecrementRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteDecrement(request);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnauthenticatedStatusException.class);
    }

    @Test
    public void voteDecrement_shouldThrowSQLExceptionOnRead() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(postsDatabase.read(anyInt(), anyInt())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteDecrement(PostVoteDecrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void voteDecrement_shouldThrowSQLExceptionOnDecrement() {
        PostVoteDecrementRequest request = PostVoteDecrementRequest.getDefaultInstance();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.decrementScore(anyInt(), anyInt())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteDecrement(request);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void voteDecrement_shouldThrowPostNotFoundError() {
        RequestSession session = RequestSession.newBuilder().setId(100).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteDecrementRequest request = PostVoteDecrementRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteDecrement(request);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostNotFoundStatusException.class);
    }

    @Test
    public void voteDecrement_shouldComplete() {
        Post post = Post
                .newBuilder()
                .setId(2)
                .build();

        PostScoreResponse exp = PostScoreResponse
                .newBuilder()
                .setId(2)
                .setScore(3)
                .setVoted(-1)
                .build();

        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);
        when(postsDatabase.decrementScore(anyInt(), anyInt())).thenReturn(exp.getScore());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteDecrementRequest request = PostVoteDecrementRequest
                .newBuilder()
                .setId(post.getId())
                .build();

        PostScoreResponse res = inProcessStub.voteDecrement(request);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());
        verify(postsDatabase).decrementScore(uidCaptor.capture(), psvdIdCaptor.capture());

        assertThat(uidCaptor.getAllValues()).containsExactly(session.getId(), session.getId());
        assertThat(psrIdCaptor.getValue()).isEqualTo(exp.getId());
        assertThat(psvdIdCaptor.getValue()).isEqualTo(exp.getId());

        assertThat(res).isNotNull();
        assertThat(res).isEqualTo(exp);
    }

    @Test
    public void voteIncrement_shouldThrowUnauthenticatedError() {
        PostVoteIncrementRequest request = PostVoteIncrementRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteIncrement(request);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        verifyZeroInteractions(postsDatabase);

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnauthenticatedStatusException.class);
    }

    @Test
    public void voteIncrement_shouldThrowPostNotFoundError() {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteIncrementRequest request = PostVoteIncrementRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteIncrement(request);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostNotFoundStatusException.class);
    }

    @Test
    public void voteIncrement_shouldThrowSQLExceptionOnIncrement() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.incrementScore(anyInt(), anyInt())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteIncrement(PostVoteIncrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void voteIncrement_shouldThrowSQLExceptionOnRead() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(postsDatabase.read(anyInt(), anyInt())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteIncrement(PostVoteIncrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void voteIncrement_shouldComplete() {
        Post post = Post
                .newBuilder()
                .setId(2)
                .build();

        PostScoreResponse exp = PostScoreResponse
                .newBuilder()
                .setId(2)
                .setScore(3)
                .setVoted(1)
                .build();

        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);
        when(postsDatabase.incrementScore(anyInt(), anyInt())).thenReturn(exp.getScore());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteIncrementRequest request = PostVoteIncrementRequest
                .newBuilder()
                .setId(post.getId())
                .build();

        PostScoreResponse res = inProcessStub.voteIncrement(request);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());
        verify(postsDatabase).incrementScore(uidCaptor.capture(), psviIdCaptor.capture());

        assertThat(uidCaptor.getAllValues()).containsExactly(session.getId(), session.getId());
        assertThat(psrIdCaptor.getValue()).isEqualTo(exp.getId());
        assertThat(psviIdCaptor.getValue()).isEqualTo(exp.getId());

        assertThat(res).isNotNull();
        assertThat(res).isEqualTo(exp);
    }

    @Test
    public void voteRemove_shouldThrowUnauthenticatedError() {
        PostVoteRemoveRequest request = PostVoteRemoveRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteRemove(request);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        verifyZeroInteractions(postsDatabase);

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnauthenticatedStatusException.class);
    }

    @Test
    public void voteRemove_shouldThrowPostNotFound() {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteRemoveRequest request = PostVoteRemoveRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteRemove(request);
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(PostNotFoundStatusException.class);
    }

    @Test
    public void voteRemove_shouldThrowSQLExceptionOnRemove() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.removeScore(anyInt(), anyInt())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteRemove(PostVoteRemoveRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void voteRemove_shouldThrowSQLExceptionOnRead() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(postsDatabase.read(anyInt(), anyInt())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteRemove(PostVoteRemoveRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            resException = ServiceError.restore(e);
        }

        assertThat(resException).isNotNull();
        assertThat(resException).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void voteRemove_shouldComplete() {
        Post post = Post
                .newBuilder()
                .setId(2)
                .build();

        PostScoreResponse exp = PostScoreResponse
                .newBuilder()
                .setId(2)
                .setScore(3)
                .setVoted(0)
                .build();

        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);
        when(postsDatabase.removeScore(anyInt(), anyInt())).thenReturn(exp.getScore());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteRemoveRequest request = PostVoteRemoveRequest
                .newBuilder()
                .setId(post.getId())
                .build();

        PostScoreResponse res = inProcessStub.voteRemove(request);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());
        verify(postsDatabase).removeScore(uidCaptor.capture(), psvrIdCaptor.capture());

        assertThat(uidCaptor.getAllValues()).containsExactly(session.getId(), session.getId());
        assertThat(psrIdCaptor.getValue()).isEqualTo(exp.getId());
        assertThat(psvrIdCaptor.getValue()).isEqualTo(exp.getId());

        assertThat(res).isNotNull();
        assertThat(res).isEqualTo(exp);
    }

    @Test
    public void readNewStream_shouldCompleteNotLoggedIn() {
        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        Iterator<Post> response = inProcessStub.readNewStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readNew(-1, request.getLimit(), request.getOffset());
    }

    @Test
    public void readNewStream_shouldCompleteLoggedIn() {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Iterator<Post> response = inProcessStub.readNewStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readNew(session.getId(), request.getLimit(), request.getOffset());
    }

    @Test
    public void readNewStream_shouldThrowSQLException() {
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new SQLException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readNewStream(PostReadListRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readNewStream_shouldThrowException() {
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new IllegalStateException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readNewStream(PostReadListRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readHotStream_shouldThrowSQLException() {
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new SQLException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readHotStream(PostReadListRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readHotStream_shouldThrowException() {
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new IllegalStateException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readHotStream(PostReadListRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readHotStream_shouldCompleteNotLoggedIn() {
        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        Iterator<Post> response = inProcessStub.readHotStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readTop(-1, request.getLimit(), request.getOffset());
    }

    @Test
    public void readHotStream_shouldCompleteLoggedIn() {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListRequest request = PostReadListRequest
                .newBuilder()
                .setLimit(5)
                .setOffset(0)
                .build();

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Iterator<Post> response = inProcessStub.readHotStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readTop(session.getId(), request.getLimit(), request.getOffset());
    }

    @Test
    public void readNewFromUserStream_shouldThrowSQLException() {
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new SQLException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readNewFromUserStream(PostReadListFromUserRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readNewFromUserStream_shouldThrowException() {
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new RuntimeException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readNewFromUserStream(PostReadListFromUserRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readNewFromUserStream_shouldCompleteLoggedIn() {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(2)
                .setLimit(5)
                .setOffset(0)
                .build();

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Iterator<Post> response = inProcessStub.readNewFromUserStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readNew(session.getId(), request.getId(), request.getLimit(), request.getOffset());
    }

    @Test
    public void readNewFromUserStream_shouldCompleteNotLoggedIn() {
        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(postsDatabase.readNew(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(2)
                .setLimit(5)
                .setOffset(0)
                .build();

        Iterator<Post> response = inProcessStub.readNewFromUserStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readNew(-1, request.getId(), request.getLimit(), request.getOffset());
    }

    @Test
    public void readTopFromUserStream_shouldThrowSQLException() {
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new SQLException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readTopFromUserStream(PostReadListFromUserRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readTopFromUserStream_shouldThrowException() {
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(new RuntimeException()));

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readTopFromUserStream(PostReadListFromUserRequest.getDefaultInstance()).next();
        } catch (StatusRuntimeException e) {
            exception = ServiceError.restore(e);
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    public void readTopFromUserStream_shouldCompleteLoggedIn() {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(2)
                .setLimit(5)
                .setOffset(0)
                .build();

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Iterator<Post> response = inProcessStub.readTopFromUserStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readTop(session.getId(), request.getId(), request.getLimit(), request.getOffset());
    }

    @Test
    public void readTopFromUserStream_shouldCompleteNotLoggedIn() {
        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3));

        PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(2)
                .setLimit(5)
                .setOffset(0)
                .build();

        Iterator<Post> response = inProcessStub.readTopFromUserStream(request);
        assertThat(response.next()).isEqualTo(post1);
        assertThat(response.next()).isEqualTo(post2);
        assertThat(response.next()).isEqualTo(post3);
        assertThat(response.hasNext()).isFalse();

        verify(postsDatabase).readTop(-1, request.getId(), request.getLimit(), request.getOffset());
    }

    @Test(timeout = 5000L)
    public void readTopFromUserStream_shouldCompleteOnEarlyDisconnect() throws InterruptedException {
        Post post1 = Post.newBuilder().setId(1).build();
        Post post2 = Post.newBuilder().setId(2).build();
        Post post3 = Post.newBuilder().setId(3).build();

        when(postsDatabase.readTop(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(Flowable.just(post1, post2, post3).delay(1, TimeUnit.SECONDS));

        final PostReadListFromUserRequest request = PostReadListFromUserRequest
                .newBuilder()
                .setId(2)
                .setLimit(5)
                .setOffset(0)
                .build();

        final List<Post> returnedPosts = new ArrayList<>();

        CountDownLatch counter = new CountDownLatch(2);

        final ClientCall<PostReadListFromUserRequest, Post> call = inProcessChannel.newCall(PostsServiceGrpc.METHOD_READ_TOP_FROM_USER_STREAM, CallOptions.DEFAULT);
        call.start(new ClientCall.Listener<Post>() {
            @Override
            public void onMessage(Post message) {
                returnedPosts.add(message);
                counter.countDown();
            }

            @Override
            public void onReady() {
                call.sendMessage(request);
                call.request(2);
                call.halfClose();
            }
        }, new Metadata());

        counter.await();

        call.cancel("No longer needed.", null);

        assertThat(returnedPosts.size()).isEqualTo(2);
    }

    @Test
    public void postScoreChangeStream_shouldComplete() throws Exception {
        List<PostScore> scores = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            scores.add(PostScore.newBuilder().setId(i).build());
        }

        List<PostScore> expScores = scores.subList(0, 5);

        when(pubSub.subPostScore()).thenReturn(Flowable.fromIterable(scores).concatMap(it -> Flowable.just(it).delay(100, TimeUnit.MILLISECONDS)));

        CountDownLatch counter = new CountDownLatch(5);

        final List<PostScore> resScores = new ArrayList<>();

        final ClientCall<PostScoreChangeRequest, PostScore> call = inProcessChannel.newCall(PostsServiceGrpc.METHOD_POST_SCORE_CHANGE_STREAM, CallOptions.DEFAULT);
        call.start(new ClientCall.Listener<PostScore>() {
            @Override
            public void onMessage(PostScore message) {
                resScores.add(message);
                counter.countDown();
                call.request(resScores.size() < 5 ? 1 : 0);
            }

            @Override
            public void onReady() {
                call.sendMessage(PostScoreChangeRequest.getDefaultInstance());
                call.halfClose();
                call.request(1);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {

            }
        }, new Metadata());

        counter.await();

        call.cancel("Cancel", null);

        assertThat(resScores).containsExactlyElementsIn(expScores);
    }

    @Test
    public void postScoreChangeStreamFiltered_shouldComplete() throws Exception {
        List<PostScore> scores = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            scores.add(PostScore.newBuilder().setId(i).build());
        }

        PostScore expScore = scores.get(5);

        when(pubSub.subPostScore()).thenReturn(Flowable.fromIterable(scores).concatMap(it -> Flowable.just(it).delay(100, TimeUnit.MILLISECONDS)));

        CountDownLatch counter = new CountDownLatch(1);

        List<PostScore> resScores = new ArrayList<>();

        final ClientCall<PostScoreChangeRequest, PostScore> call = inProcessChannel.newCall(PostsServiceGrpc.METHOD_POST_SCORE_CHANGE_STREAM, CallOptions.DEFAULT);
        call.start(new ClientCall.Listener<PostScore>() {
            @Override
            public void onMessage(PostScore message) {
                resScores.add(message);
                counter.countDown();
                call.request(1);
            }

            @Override
            public void onReady() {
                call.sendMessage(PostScoreChangeRequest.newBuilder().setId(expScore.getId()).build());
                call.halfClose();
                call.request(1);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {

            }
        }, new Metadata());

        counter.await();

        call.cancel("Cancel", null);

        assertThat(resScores).hasSize(1);
        assertThat(resScores.get(0)).isEqualTo(expScore);
    }

    @Test
    public void voteIncrement_shouldPublishEvent() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.incrementScore(anyInt(), anyInt())).thenReturn(10);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteIncrementRequest request = PostVoteIncrementRequest.getDefaultInstance();

        inProcessStub.voteIncrement(request);

        PostScore expect = PostScore.newBuilder().setId(0).setScore(10).build();

        ArgumentCaptor<PostScore> captor = ArgumentCaptor.forClass(PostScore.class);
        verify(pubSub).pubPostScore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expect);
    }

    @Test
    public void voteDecrement_shouldPublishEvent() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.decrementScore(anyInt(), anyInt())).thenReturn(10);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteDecrementRequest request = PostVoteDecrementRequest.getDefaultInstance();

        inProcessStub.voteDecrement(request);

        PostScore expect = PostScore.newBuilder().setId(0).setScore(10).build();

        ArgumentCaptor<PostScore> captor = ArgumentCaptor.forClass(PostScore.class);
        verify(pubSub).pubPostScore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expect);
    }

    @Test
    public void voteRemove_shouldPublishEvent() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(Post.getDefaultInstance());
        when(postsDatabase.removeScore(anyInt(), anyInt())).thenReturn(10);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteRemoveRequest request = PostVoteRemoveRequest.getDefaultInstance();

        inProcessStub.voteRemove(request);

        PostScore expect = PostScore.newBuilder().setId(0).setScore(10).build();

        ArgumentCaptor<PostScore> captor = ArgumentCaptor.forClass(PostScore.class);
        verify(pubSub).pubPostScore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expect);
    }

}
