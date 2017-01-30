package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.CommentsDatabase;
import com.asadmshah.hnclone.errors.CommentsServiceErrors;
import com.asadmshah.hnclone.errors.CommonServiceErrors;
import com.asadmshah.hnclone.models.Comment;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor;
import com.asadmshah.hnclone.services.CommentCreateRequest;
import com.asadmshah.hnclone.services.CommentsServiceGrpc;
import com.asadmshah.hnclone.services.CommentsServiceGrpc.CommentsServiceBlockingStub;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommentsServiceEndpointTest {

    private static final String SERVER_NAME = "in-process server for " + CommentsServiceEndpoint.class.getSimpleName();

    private ManagedChannel inProcessChannel;
    private Server inProcessServer;

    private CommentsServiceBlockingStub inProcessStub;

    @Mock private SessionManager sessionManager;
    @Mock private CommentsDatabase commentsDatabase;
    @Mock private ServerComponent component;
//    @Mock private PubSub pubSub;
    @Mock private BlockedSessionsCache blockedSessionsCache;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.commentsDatabase()).thenReturn(commentsDatabase);
//        when(component.pubSub()).thenReturn(pubSub);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);

        inProcessChannel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        inProcessServer = InProcessServerBuilder
                .forName(SERVER_NAME)
                .addService(CommentsServiceEndpoint.create(component))
                .directExecutor()
                .build();
        inProcessServer.start();

        inProcessStub = CommentsServiceGrpc.newBlockingStub(inProcessChannel);
    }

    @After
    public void tearDown() throws Exception {
        inProcessChannel.shutdownNow();
        inProcessServer.shutdownNow();
    }

    @Test
    public void create_shouldThrowErrorOnNoSession() throws Exception {
        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(CommentCreateRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION.getMessage());
    }

    @Test
    public void create_shouldThrowErrorOnEmptyText() throws Exception {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setText("    ")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommentsServiceErrors.TEXT_REQUIRED_EXCEPTION.getMessage());
    }

    @Test
    public void create_shouldThrowErrorOnLongText() throws Exception {
        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setText(StringUtils.repeat("A", 1025))
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommentsServiceErrors.TEXT_TOO_LONG_EXCEPTION.getMessage());
    }

    @Test
    public void create_shouldThrowSQLException() throws Exception {
        when(commentsDatabase.create(anyInt(), anyInt(), anyString())).thenThrow(SQLException.class);

        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setText("Test Comment")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void create_shouldThrowUnknownErrorOnNullResult() throws Exception {
        when(commentsDatabase.create(anyInt(), anyInt(), anyString())).thenReturn(null);

        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setText("Test Comment")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void create_shouldCompleteParentCreate() throws Exception {
        Comment expComment = Comment
                .newBuilder()
                .setId(25)
                .build();

        when(commentsDatabase.create(anyInt(), anyInt(), anyString())).thenReturn(expComment);

        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setPostId(10)
                .setText("Test Comment")
                .build();

        Comment resComment = inProcessStub.create(request);

        assertThat(resComment).isEqualTo(expComment);

        verify(commentsDatabase).create(session.getId(), request.getPostId(), request.getText());
        verify(commentsDatabase, never()).create(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    public void create_shouldCompleteChildCreate() throws Exception {
        Comment expComment = Comment
                .newBuilder()
                .setId(25)
                .build();

        when(commentsDatabase.create(anyInt(), anyInt(), anyInt(), anyString())).thenReturn(expComment);

        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .setText("Test Comment")
                .build();

        Comment resComment = inProcessStub.create(request);

        assertThat(resComment).isEqualTo(expComment);

        verify(commentsDatabase).create(session.getId(), request.getPostId(), request.getCommentId(), request.getText());
        verify(commentsDatabase, never()).create(anyInt(), anyInt(), anyString());
    }

    @Test
    public void create_shouldEscapeText() throws Exception {
        when(commentsDatabase.create(anyInt(), anyInt(), anyString())).thenReturn(Comment.getDefaultInstance());

        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setText("<script>alert(document.cookie);</script>")
                .build();

        inProcessStub.create(request);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(commentsDatabase).create(anyInt(), anyInt(), textCaptor.capture());

        assertThat(textCaptor.getValue()).isEqualTo("&lt;script&gt;alert(document.cookie);&lt;\\/script&gt;");
    }
}
