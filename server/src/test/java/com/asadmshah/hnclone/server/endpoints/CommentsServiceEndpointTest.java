package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.CommentsDatabase;
import com.asadmshah.hnclone.errors.CommentsServiceErrors;
import com.asadmshah.hnclone.errors.CommonServiceErrors;
import com.asadmshah.hnclone.models.Comment;
import com.asadmshah.hnclone.models.CommentScore;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.pubsub.PubSub;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor;
import com.asadmshah.hnclone.services.*;
import com.asadmshah.hnclone.services.CommentsServiceGrpc.CommentsServiceBlockingStub;
import com.google.common.collect.Lists;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    @Mock private PubSub pubSub;
    @Mock private BlockedSessionsCache blockedSessionsCache;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.commentsDatabase()).thenReturn(commentsDatabase);
        when(component.pubSub()).thenReturn(pubSub);
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

    @Test
    public void create_shouldPublishComment() throws Exception {
        when(commentsDatabase.create(anyInt(), anyInt(), anyString())).thenReturn(Comment.getDefaultInstance());

        RequestSession session = RequestSession.getDefaultInstance();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentCreateRequest request = CommentCreateRequest
                .newBuilder()
                .setText("Text")
                .build();

        inProcessStub.create(request);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(pubSub).pubComment(commentCaptor.capture());
        assertThat(commentCaptor.getValue()).isEqualTo(Comment.getDefaultInstance());
    }

    @Test
    public void read_shouldThrowSQLException() throws Exception {
        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.read(CommentReadRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void read_shouldThrowNotFound() throws Exception {
        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.read(CommentReadRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommentsServiceErrors.NOT_FOUND_EXCEPTION.getMessage());
    }

    @Test
    public void read_shouldCompleteNotLoggedIn() throws Exception {
        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());

        CommentReadRequest request = CommentReadRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        inProcessStub.read(request);

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> cidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComment(vidCaptor.capture(), pidCaptor.capture(), cidCaptor.capture());

        assertThat(vidCaptor.getValue()).isLessThan(1);
        assertThat(pidCaptor.getValue()).isEqualTo(request.getPostId());
        assertThat(cidCaptor.getValue()).isEqualTo(request.getCommentId());
    }

    @Test
    public void read_shouldCompleteLoggedIn() throws Exception {
        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());

        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        CommentReadRequest request = CommentReadRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        inProcessStub.read(request);

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> cidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComment(vidCaptor.capture(), pidCaptor.capture(), cidCaptor.capture());

        assertThat(vidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(pidCaptor.getValue()).isEqualTo(request.getPostId());
        assertThat(cidCaptor.getValue()).isEqualTo(request.getCommentId());
    }

    @Test
    public void readListFromPost_shouldThrowSQLException() throws Exception {
        when(commentsDatabase.readComments(anyInt(), anyInt())).thenReturn(Flowable.error(SQLException::new));

        StatusRuntimeException exception = null;
        try {
            Iterator<Comment> response = inProcessStub.readListFromPost(CommentReadListFromPostRequest.getDefaultInstance());
            response.next();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void readListFromPost_shouldCompleteNotLoggedIn() throws Exception {
        Comment comment1 = Comment.newBuilder().setId(1).build();
        Comment comment2 = Comment.newBuilder().setId(2).build();
        Comment comment3 = Comment.newBuilder().setId(3).build();

        List<Comment> expComments = Arrays.asList(comment1, comment2, comment3);

        when(commentsDatabase.readComments(anyInt(), anyInt())).thenReturn(Flowable.fromIterable(expComments));

        CommentReadListFromPostRequest request = CommentReadListFromPostRequest
                .newBuilder()
                .setPostId(10)
                .build();

        List<Comment> resComments = Lists.newArrayList(inProcessStub.readListFromPost(request));

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComments(vidCaptor.capture(), pidCaptor.capture());
        assertThat(vidCaptor.getValue()).isLessThan(1);
        assertThat(pidCaptor.getValue()).isEqualTo(request.getPostId());
    }

    @Test
    public void readListFromPost_shouldCompleteLoggedIn() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Comment comment1 = Comment.newBuilder().setId(1).build();
        Comment comment2 = Comment.newBuilder().setId(2).build();
        Comment comment3 = Comment.newBuilder().setId(3).build();

        List<Comment> expComments = Arrays.asList(comment1, comment2, comment3);

        when(commentsDatabase.readComments(anyInt(), anyInt())).thenReturn(Flowable.fromIterable(expComments));

        CommentReadListFromPostRequest request = CommentReadListFromPostRequest
                .newBuilder()
                .setPostId(10)
                .build();

        List<Comment> resComments = Lists.newArrayList(inProcessStub.readListFromPost(request));

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComments(vidCaptor.capture(), pidCaptor.capture());
        assertThat(vidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(pidCaptor.getValue()).isEqualTo(request.getPostId());
    }

    @Test
    public void readListFromComment_shouldThrowSQLException() throws Exception {
        when(commentsDatabase.readComments(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.error(SQLException::new));

        StatusRuntimeException exception = null;
        try {
            Iterator<Comment> response = inProcessStub.readListFromComment(CommentReadListFromCommentRequest.getDefaultInstance());
            response.next();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void readListFromComment_shouldCompleteNotLoggedIn() throws Exception {
        Comment comment1 = Comment.newBuilder().setId(1).build();
        Comment comment2 = Comment.newBuilder().setId(2).build();
        Comment comment3 = Comment.newBuilder().setId(3).build();

        List<Comment> expComments = Arrays.asList(comment1, comment2, comment3);

        when(commentsDatabase.readComments(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.fromIterable(expComments));

        CommentReadListFromCommentRequest request = CommentReadListFromCommentRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        List<Comment> resComments = Lists.newArrayList(inProcessStub.readListFromComment(request));

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> cidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComments(vidCaptor.capture(), pidCaptor.capture(), cidCaptor.capture());
        assertThat(vidCaptor.getValue()).isLessThan(1);
        assertThat(pidCaptor.getValue()).isEqualTo(request.getPostId());
        assertThat(cidCaptor.getValue()).isEqualTo(request.getCommentId());
    }

    @Test
    public void readListFromComment_shouldCompleteLoggedIn() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        Comment comment1 = Comment.newBuilder().setId(1).build();
        Comment comment2 = Comment.newBuilder().setId(2).build();
        Comment comment3 = Comment.newBuilder().setId(3).build();

        List<Comment> expComments = Arrays.asList(comment1, comment2, comment3);

        when(commentsDatabase.readComments(anyInt(), anyInt(), anyInt())).thenReturn(Flowable.fromIterable(expComments));

        CommentReadListFromCommentRequest request = CommentReadListFromCommentRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        List<Comment> resComments = Lists.newArrayList(inProcessStub.readListFromComment(request));

        assertThat(resComments).containsExactlyElementsIn(expComments).inOrder();

        ArgumentCaptor<Integer> vidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> pidCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> cidCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(commentsDatabase).readComments(vidCaptor.capture(), pidCaptor.capture(), cidCaptor.capture());
        assertThat(vidCaptor.getValue()).isEqualTo(session.getId());
        assertThat(pidCaptor.getValue()).isEqualTo(request.getPostId());
        assertThat(cidCaptor.getValue()).isEqualTo(request.getCommentId());
    }

    @Test
    public void voteIncrement_shouldThrowUnauthenticatedException() throws Exception {
        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteIncrement(CommentVoteIncrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION.getMessage());
    }

    @Test
    public void voteIncrement_shouldThrowSQLExceptionOnReadComment() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteIncrement(CommentVoteIncrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteIncrement_shouldThrowNotFoundExceptionOnReadComment() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteIncrement(CommentVoteIncrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommentsServiceErrors.NOT_FOUND_EXCEPTION.getMessage());
    }

    @Test
    public void voteIncrement_shouldThrowSQLExceptionOnIncrementScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.incrementScore(anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteIncrement(CommentVoteIncrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteIncrement_shouldThrowUnknownExceptionOnIncrementScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.incrementScore(anyInt(), anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteIncrement(CommentVoteIncrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteIncrement_shouldComplete() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.incrementScore(anyInt(), anyInt())).thenReturn(10);

        CommentVoteIncrementRequest request = CommentVoteIncrementRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        CommentScoreResponse response = inProcessStub.voteIncrement(request);

        assertThat(response.getPostId()).isEqualTo(10);
        assertThat(response.getCommentId()).isEqualTo(20);
        assertThat(response.getScore()).isEqualTo(10);
        assertThat(response.getVoted()).isEqualTo(1);

        verify(commentsDatabase).incrementScore(session.getId(), request.getCommentId());
    }

    @Test
    public void voteIncrement_shouldPublishCommentScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.incrementScore(anyInt(), anyInt())).thenReturn(10);

        CommentVoteIncrementRequest request = CommentVoteIncrementRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        inProcessStub.voteIncrement(request);

        CommentScore expPublishedScore = CommentScore
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .setScore(10)
                .build();

        ArgumentCaptor<CommentScore> captor = ArgumentCaptor.forClass(CommentScore.class);
        verify(pubSub).pubCommentScore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expPublishedScore);
    }

    @Test
    public void voteDecrement_shouldThrowUnauthenticatedException() throws Exception {
        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteDecrement(CommentVoteDecrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION.getMessage());
    }

    @Test
    public void voteDecrement_shouldThrowSQLExceptionOnReadComment() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteDecrement(CommentVoteDecrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteDecrement_shouldThrowNotFoundExceptionOnReadComment() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteDecrement(CommentVoteDecrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommentsServiceErrors.NOT_FOUND_EXCEPTION.getMessage());
    }

    @Test
    public void voteDecrement_shouldThrowSQLExceptionOnDecrementScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.decrementScore(anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteDecrement(CommentVoteDecrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteDecrement_shouldThrowUnknownExceptionOnDecrementScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.decrementScore(anyInt(), anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteDecrement(CommentVoteDecrementRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteDecrement_shouldComplete() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.decrementScore(anyInt(), anyInt())).thenReturn(10);

        CommentVoteDecrementRequest request = CommentVoteDecrementRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        CommentScoreResponse response = inProcessStub.voteDecrement(request);

        assertThat(response.getPostId()).isEqualTo(10);
        assertThat(response.getCommentId()).isEqualTo(20);
        assertThat(response.getScore()).isEqualTo(10);
        assertThat(response.getVoted()).isEqualTo(-1);

        verify(commentsDatabase).decrementScore(session.getId(), request.getCommentId());
    }

    @Test
    public void voteDecrement_shouldPublishCommentScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.decrementScore(anyInt(), anyInt())).thenReturn(10);

        CommentVoteDecrementRequest request = CommentVoteDecrementRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        inProcessStub.voteDecrement(request);

        CommentScore expPublishedScore = CommentScore
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .setScore(10)
                .build();

        ArgumentCaptor<CommentScore> captor = ArgumentCaptor.forClass(CommentScore.class);
        verify(pubSub).pubCommentScore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expPublishedScore);
    }

    @Test
    public void voteRemove_shouldThrowUnauthenticatedException() throws Exception {
        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteRemove(CommentVoteRemoveRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNAUTHENTICATED_EXCEPTION.getMessage());
    }

    @Test
    public void voteRemove_shouldThrowSQLExceptionOnReadComment() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteRemove(CommentVoteRemoveRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteRemove_shouldThrowNotFoundExceptionOnReadComment() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteRemove(CommentVoteRemoveRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommentsServiceErrors.NOT_FOUND_EXCEPTION.getMessage());
    }

    @Test
    public void voteRemove_shouldThrowSQLExceptionOnDecrementScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.removeScore(anyInt(), anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteRemove(CommentVoteRemoveRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteRemove_shouldThrowUnknownExceptionOnDecrementScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.removeScore(anyInt(), anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.voteRemove(CommentVoteRemoveRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void voteRemove_shouldComplete() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.removeScore(anyInt(), anyInt())).thenReturn(10);

        CommentVoteRemoveRequest request = CommentVoteRemoveRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        CommentScoreResponse response = inProcessStub.voteRemove(request);

        assertThat(response.getPostId()).isEqualTo(10);
        assertThat(response.getCommentId()).isEqualTo(20);
        assertThat(response.getScore()).isEqualTo(10);
        assertThat(response.getVoted()).isEqualTo(0);

        verify(commentsDatabase).removeScore(session.getId(), request.getCommentId());
    }

    @Test
    public void commentsStream_shouldThrowError() throws Exception {
        when(pubSub.subComments()).thenReturn(Flowable.error(RuntimeException::new));

        StatusRuntimeException exception = null;
        try {
            Iterator<Comment> iterator = inProcessStub.commentStream(CommentStreamRequest.getDefaultInstance());
            iterator.next();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void commentsStream_shouldComplete() throws Exception {
        Comment comment1 = Comment.newBuilder().setPostId(1).setId(1).build();
        Comment comment2 = Comment.newBuilder().setPostId(2).setId(2).build();
        Comment comment3 = Comment.newBuilder().setPostId(1).setId(3).build();
        Comment comment4 = Comment.newBuilder().setPostId(2).setId(4).build();
        Comment comment5 = Comment.newBuilder().setPostId(1).setId(5).build();

        when(pubSub.subComments()).thenReturn(Flowable.just(comment1, comment2, comment3, comment4, comment5).delay(100, TimeUnit.MILLISECONDS));

        CommentStreamRequest request = CommentStreamRequest.newBuilder().setPostId(1).build();

        List<Comment> response = Lists.newArrayList(inProcessStub.commentStream(request));

        assertThat(response).containsExactly(comment1, comment3, comment5).inOrder();
    }

    @Test
    public void commentScoresStream_shouldThrowError() throws Exception {
        when(pubSub.subCommentScores()).thenReturn(Flowable.error(RuntimeException::new));

        StatusRuntimeException exception = null;
        try {
            Iterator<CommentScore> iterator = inProcessStub.commentScoreStream(CommentScoreStreamRequest.getDefaultInstance());
            iterator.next();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).hasMessage(CommonServiceErrors.UNKNOWN_EXCEPTION.getMessage());
    }

    @Test
    public void commentScoresStream_shouldComplete() throws Exception {
        CommentScore cs1 = CommentScore.newBuilder().setPostId(1).setCommentId(1).build();
        CommentScore cs2 = CommentScore.newBuilder().setPostId(2).setCommentId(2).build();
        CommentScore cs3 = CommentScore.newBuilder().setPostId(1).setCommentId(3).build();
        CommentScore cs4 = CommentScore.newBuilder().setPostId(2).setCommentId(4).build();
        CommentScore cs5 = CommentScore.newBuilder().setPostId(1).setCommentId(5).build();

        when(pubSub.subCommentScores()).thenReturn(Flowable.just(cs1, cs2, cs3, cs4, cs5).delay(100, TimeUnit.MILLISECONDS));

        CommentScoreStreamRequest request = CommentScoreStreamRequest.newBuilder().setPostId(1).build();

        List<CommentScore> response = Lists.newArrayList(inProcessStub.commentScoreStream(request));

        assertThat(response).containsExactly(cs1, cs3, cs5).inOrder();
    }

    @Test
    public void voteRemove_shouldPublishCommentScore() throws Exception {
        RequestSession session = RequestSession.newBuilder().setId(100).build();
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        when(commentsDatabase.readComment(anyInt(), anyInt(), anyInt())).thenReturn(Comment.getDefaultInstance());
        when(commentsDatabase.removeScore(anyInt(), anyInt())).thenReturn(10);

        CommentVoteRemoveRequest request = CommentVoteRemoveRequest
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .build();

        inProcessStub.voteRemove(request);

        CommentScore expPublishedScore = CommentScore
                .newBuilder()
                .setPostId(10)
                .setCommentId(20)
                .setScore(10)
                .build();

        ArgumentCaptor<CommentScore> captor = ArgumentCaptor.forClass(CommentScore.class);
        verify(pubSub).pubCommentScore(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expPublishedScore);
    }
}
