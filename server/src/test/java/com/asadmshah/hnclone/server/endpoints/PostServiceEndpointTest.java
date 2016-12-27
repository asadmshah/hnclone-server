package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.common.tools.StringExtKt;
import com.asadmshah.hnclone.errors.CommonServiceErrors;
import com.asadmshah.hnclone.errors.PostServiceErrors;
import com.asadmshah.hnclone.models.*;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.database.PostsDatabase;
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor;
import com.asadmshah.hnclone.services.PostServiceGrpc;
import com.asadmshah.hnclone.services.PostServiceGrpc.PostServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostServiceEndpointTest {

    private static final String SERVER_NAME = "in-process server for " + PostServiceEndpoint.class.getSimpleName();

    private ManagedChannel inProcessChannel;
    private Server inProcessServer;

    private PostServiceBlockingStub inProcessStub;

    @Mock private SessionManager sessionManager;
    @Mock private PostsDatabase postsDatabase;
    @Mock private ServerComponent component;

    @Captor private ArgumentCaptor<Integer> uidCaptor;
    @Captor private ArgumentCaptor<String> pscTitleCaptor;
    @Captor private ArgumentCaptor<String> pscUrlCaptor;
    @Captor private ArgumentCaptor<String> pscTextCaptor;
    @Captor private ArgumentCaptor<Integer> psdIdCaptor;
    @Captor private ArgumentCaptor<Integer> psrIdCaptor;
    @Captor private ArgumentCaptor<Integer> psvdIdCaptor;
    @Captor private ArgumentCaptor<Integer> psviIdCaptor;
    @Captor private ArgumentCaptor<Integer> psvrIdCaptor;

    private Session testSession;

    @Before
    public void setUp() throws Exception {
//        when(component.usersDatabase()).thenReturn(usersDatabase);
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.postsDatabase()).thenReturn(postsDatabase);

        inProcessChannel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        inProcessServer = InProcessServerBuilder
                .forName(SERVER_NAME)
                .addService(PostServiceEndpoint.create(component))
                .directExecutor()
                .build();

        inProcessServer.start();

        inProcessStub = PostServiceGrpc.newBlockingStub(inProcessChannel);

        testSession = Session.newBuilder().setId(10).build();
    }

    @After
    public void tearDown() throws Exception {
        inProcessChannel.shutdownNow();
        inProcessServer.shutdownNow();
    }

    @Test
    public void createPost_shouldComplete() {
        Post expPost = Post.getDefaultInstance();

        when(sessionManager.parseRequestToken(anyString()))
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
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Post resPost = inProcessStub.create(req);

        verify(postsDatabase).create(uidCaptor.capture(), pscTitleCaptor.capture(), pscUrlCaptor.capture(), pscTextCaptor.capture());

        assertThat(resPost).isNotNull();
        assertThat(resPost).isEqualTo(expPost);

        assertThat(uidCaptor.getValue()).isEqualTo(testSession.getId());
        assertThat(pscTitleCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getTitle()));
        assertThat(pscUrlCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getUrl()));
        assertThat(pscTextCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getText()));
    }

    @Test
    public void createPost_shouldThrowErrorOnNoSession() {
//        Session session = Session.getDefaultInstance();
//        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Post post = Post.getDefaultInstance();

//        when(postsDatabase.create(anyInt(), anyString(), anyString(), anyString()))
//                .thenReturn(post);

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
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnauthenticated().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorOnEmptyTitle() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("")
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getTitleRequired().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorOnNullTitle() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getTitleRequired().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorOnLongTitle() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle(StringUtils.repeat("T", 129))
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getTitleTooLong().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorOnNoContent() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .build();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.create(req);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        verify(postsDatabase, never()).create(anyInt(), anyString(), anyString(), anyString());

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getContentRequired().getDescription());
    }

    @Test
    public void createPost_shouldSucceedWithTitleAndUrlButNoText() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Post expPost = Post.getDefaultInstance();

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setUrl("http://www.google.com")
                .build();

        when(postsDatabase.create(anyInt(), anyString(), anyString(), isNull()))
                .thenReturn(expPost);

        Post resPost = inProcessStub.create(req);

        verify(postsDatabase).create(uidCaptor.capture(), pscTitleCaptor.capture(), pscUrlCaptor.capture(), pscTextCaptor.capture());

        assertThat(resPost).isNotNull();
        assertThat(resPost).isEqualTo(expPost);

        assertThat(uidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(pscTitleCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getTitle()));
        assertThat(pscUrlCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getUrl()));
        assertThat(pscTextCaptor.getValue()).isNull();
    }

    @Test
    public void createPost_shouldSucceedWithTitleAndTextButNoUrl() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Post expPost = Post.getDefaultInstance();

        PostCreateRequest req = PostCreateRequest
                .newBuilder()
                .setTitle("Test Title")
                .setText("Test Text")
                .build();

        when(postsDatabase.create(anyInt(), anyString(), isNull(), anyString()))
                .thenReturn(expPost);

        Post resPost = inProcessStub.create(req);

        verify(postsDatabase).create(uidCaptor.capture(), pscTitleCaptor.capture(), pscUrlCaptor.capture(), pscTextCaptor.capture());

        assertThat(resPost).isNotNull();
        assertThat(resPost).isEqualTo(expPost);

        assertThat(uidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(pscTitleCaptor.getValue()).isEqualTo(StringExtKt.escape(req.getTitle()));
        assertThat(pscUrlCaptor.getValue()).isNull();
        assertThat(pscTextCaptor.getValue()).isEqualTo(req.getText());
    }

    @Test
    public void createPost_shouldThrowErrorOnInvalidURL() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
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
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getContentURLInvalid().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorTextTooLong() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
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
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getContentTextTooLong().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorOnDatabaseError() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
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
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnknown().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorOnCreateReturningNull() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
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
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnknown().getDescription());
    }

    @Test
    public void createPost_shouldThrowErrorOnLongUrl() {
        Session session = Session.getDefaultInstance();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
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
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getContentURLUnacceptable().getDescription());
    }

    @Test
    public void deletePost_shouldThrowUnauthenticatedError() {
        PostDeleteRequest req = PostDeleteRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.delete(req);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnauthenticated().getDescription());
    }

    @Test
    public void deletePost_shouldThrowPostNotFoundError() {
        Session session = Session.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostDeleteRequest req = PostDeleteRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.delete(req);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getNotFound().getDescription());
    }

    @Test
    public void deletePost_shouldThrowUnauthorizedError() {
        Session session = Session.newBuilder().setId(1).build();
        Post post = Post.newBuilder().setUserId(2).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostDeleteRequest req = PostDeleteRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.delete(req);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnauthorized().getDescription());
    }

    @Test
    public void deletePost_shouldComplete() {
        int userId = 10;
        int postId = 20;

        Session session = Session.newBuilder().setId(userId).build();
        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Post post = Post.newBuilder().setId(postId).setUserId(userId).build();
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);

        when(postsDatabase.delete(anyInt())).thenReturn(true);

        PostDeleteRequest req = PostDeleteRequest
                .newBuilder()
                .setId(post.getId())
                .build();

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostDeleteResponse res = inProcessStub.delete(req);

        verify(postsDatabase).read(uidCaptor.capture(), psdIdCaptor.capture());
        verify(postsDatabase).delete(psdIdCaptor.capture());

        assertThat(uidCaptor.getValue()).isEqualTo(userId);
        assertThat(psdIdCaptor.getAllValues()).containsExactly(postId, postId);

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(req.getId());
        assertThat(res.getDeleted()).isTrue();
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
        Session session = Session.newBuilder().setId(100).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);

        Post exp = Post
                .newBuilder()
                .setId(10)
                .build();

        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(exp);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
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
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getNotFound().getDescription());
    }

    @Test
    public void voteDecrement_shouldThrowUnauthenticatedError() {
        PostVoteDecrementRequest request = PostVoteDecrementRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteDecrement(request);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnauthenticated().getDescription());
    }

    @Test
    public void voteDecrement_shouldThrowPostNotFoundError() {
        Session session = Session.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteDecrementRequest request = PostVoteDecrementRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteDecrement(request);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getNotFound().getDescription());
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

        Session session = Session.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);
        when(postsDatabase.voteDecrement(anyInt(), anyInt())).thenReturn(exp.getScore());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteDecrementRequest request = PostVoteDecrementRequest
                .newBuilder()
                .setId(post.getId())
                .build();

        PostScoreResponse res = inProcessStub.voteDecrement(request);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());
        verify(postsDatabase).voteDecrement(uidCaptor.capture(), psvdIdCaptor.capture());

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
            resException = e;
        }

        verifyZeroInteractions(postsDatabase);

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnauthenticated().getDescription());
    }

    @Test
    public void voteIncrement_shouldThrowPostNotFoundError() {
        Session session = Session.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteIncrementRequest request = PostVoteIncrementRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteIncrement(request);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getNotFound().getDescription());
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

        Session session = Session.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);
        when(postsDatabase.voteIncrement(anyInt(), anyInt())).thenReturn(exp.getScore());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteIncrementRequest request = PostVoteIncrementRequest
                .newBuilder()
                .setId(post.getId())
                .build();

        PostScoreResponse res = inProcessStub.voteIncrement(request);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());
        verify(postsDatabase).voteIncrement(uidCaptor.capture(), psviIdCaptor.capture());

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
            resException = e;
        }

        verifyZeroInteractions(postsDatabase);

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(CommonServiceErrors.INSTANCE.getUnauthenticated().getDescription());
    }

    @Test
    public void voteRemove_shouldThrowPostNotFound() {
        Session session = Session.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteRemoveRequest request = PostVoteRemoveRequest.getDefaultInstance();

        StatusRuntimeException resException = null;
        try {
            inProcessStub.voteRemove(request);
        } catch (StatusRuntimeException e) {
            resException = e;
        }

        assertThat(resException).isNotNull();
        assertThat(resException.getStatus().getDescription()).isEqualTo(PostServiceErrors.INSTANCE.getNotFound().getDescription());
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

        Session session = Session.newBuilder().setId(1).build();

        when(sessionManager.parseRequestToken(anyString())).thenReturn(session);
        when(postsDatabase.read(anyInt(), anyInt())).thenReturn(post);
        when(postsDatabase.voteRemove(anyInt(), anyInt())).thenReturn(exp.getScore());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), "");
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        PostVoteRemoveRequest request = PostVoteRemoveRequest
                .newBuilder()
                .setId(post.getId())
                .build();

        PostScoreResponse res = inProcessStub.voteRemove(request);

        verify(postsDatabase).read(uidCaptor.capture(), psrIdCaptor.capture());
        verify(postsDatabase).voteRemove(uidCaptor.capture(), psvrIdCaptor.capture());

        assertThat(uidCaptor.getAllValues()).containsExactly(session.getId(), session.getId());
        assertThat(psrIdCaptor.getValue()).isEqualTo(exp.getId());
        assertThat(psvrIdCaptor.getValue()).isEqualTo(exp.getId());

        assertThat(res).isNotNull();
        assertThat(res).isEqualTo(exp);
    }

}
