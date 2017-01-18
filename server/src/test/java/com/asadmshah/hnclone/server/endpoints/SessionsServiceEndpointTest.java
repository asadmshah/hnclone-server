package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.ExpiredTokenException;
import com.asadmshah.hnclone.common.sessions.InvalidTokenException;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.SessionsDatabase;
import com.asadmshah.hnclone.database.UsersDatabase;
import com.asadmshah.hnclone.errors.CommonServiceErrors;
import com.asadmshah.hnclone.errors.SessionsServiceErrors;
import com.asadmshah.hnclone.models.RefreshSession;
import com.asadmshah.hnclone.models.SessionToken;
import com.asadmshah.hnclone.models.User;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.services.SessionCreateRequest;
import com.asadmshah.hnclone.services.SessionCreateResponse;
import com.asadmshah.hnclone.services.SessionsServiceGrpc;
import com.asadmshah.hnclone.services.SessionsServiceGrpc.SessionsServiceBlockingStub;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SessionsServiceEndpointTest {

    private static final String UNIQUE_SERVER_NAME = "in-process-server for " + SessionsServiceEndpointTest.class.getSimpleName();

    private ManagedChannel inProcessChannel = InProcessChannelBuilder
            .forName(UNIQUE_SERVER_NAME)
            .directExecutor()
            .build();

    private Server inProcessServer;

    private SessionsServiceBlockingStub inProcessStub;

    @Mock private SessionManager sessionManager;
    @Mock private UsersDatabase usersDatabase;
    @Mock private SessionsDatabase sessionsDatabase;
    @Mock private BlockedSessionsCache blockedSessionsCache;
    @Mock private ServerComponent component;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);
        when(component.sessionsDatabase()).thenReturn(sessionsDatabase);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);

        inProcessServer = InProcessServerBuilder
                .forName(UNIQUE_SERVER_NAME)
                .addService(SessionsServiceEndpoint.create(component))
                .directExecutor()
                .build();

        inProcessServer.start();

        inProcessStub = SessionsServiceGrpc.newBlockingStub(inProcessChannel);
    }

    @After
    public void tearDown() {
        inProcessChannel.shutdownNow();
        inProcessServer.shutdownNow();
    }

    @Test
    public void refresh_shouldThrowExpiredTokenException() {
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenThrow(ExpiredTokenException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.refresh(SessionToken.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(SessionsServiceErrors.EXPIRED_TOKEN.getDescription());
    }

    @Test
    public void refresh_shouldThrowInvalidTokenException() {
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenThrow(InvalidTokenException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.refresh(SessionToken.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(SessionsServiceErrors.INVALID_TOKEN.getDescription());
    }

    @Test
    public void refresh_shouldFailOnNoSessions() {
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenReturn(RefreshSession.getDefaultInstance());
        when(sessionsDatabase.read(anyString())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.refresh(SessionToken.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(SessionsServiceErrors.INVALID_TOKEN.getDescription());
    }

    @Test
    public void refresh_shouldComplete() {
        SessionToken refreshToken = createSessionToken("a1", "a2");
        SessionToken expRequestToken = createSessionToken("b1", "b2");
        RefreshSession refreshSession = RefreshSession.newBuilder().setId(10).build();

        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenReturn(refreshSession);
        when(sessionManager.createRequestToken(anyInt())).thenReturn(expRequestToken);
        when(sessionsDatabase.read(anyString())).thenReturn(refreshSession);
        when(blockedSessionsCache.contains(anyInt(), any(LocalDateTime.class))).thenReturn(false);

        SessionToken resRequestToken = inProcessStub.refresh(refreshToken);

        verify(sessionManager).parseRefreshToken(refreshToken);
        verify(sessionManager).createRequestToken(10);
        verify(blockedSessionsCache).contains(refreshSession.getId(), LocalDateTime.ofInstant(Instant.ofEpochMilli(refreshSession.getIssued()), ZoneOffset.UTC));

        assertThat(resRequestToken).isNotNull();
        assertThat(resRequestToken).isEqualTo(expRequestToken);
    }

    @Test
    public void create_shouldReturnUserNotFoundException() {
        SessionCreateRequest request = SessionCreateRequest.getDefaultInstance();

        when(usersDatabase.read(anyString(), anyString())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(SessionsServiceErrors.USER_NOT_FOUND.getDescription());
    }

    @Test
    public void create_shouldThrowSQLException() {
        SessionCreateRequest request = SessionCreateRequest.getDefaultInstance();

        when(usersDatabase.read(anyString(), anyString())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNKNOWN.getDescription());
    }

    @Test
    public void create_shouldComplete() {
        SessionCreateRequest request = SessionCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

        User user = User.newBuilder().setId(10).build();

        SessionToken requestToken = SessionToken.newBuilder().setData(ByteString.copyFromUtf8("Request")).build();
        SessionToken refreshToken = SessionToken.newBuilder().setData(ByteString.copyFromUtf8("Refresh")).build();

        when(usersDatabase.read(anyString(), anyString())).thenReturn(user);
        when(sessionManager.createRequestToken(anyInt())).thenReturn(requestToken);
        when(sessionManager.createRefreshToken(anyInt())).thenReturn(refreshToken);

        SessionCreateResponse response = inProcessStub.create(request);

        verify(usersDatabase).read(request.getUsername(), request.getPassword());
        verify(sessionManager).createRequestToken(user.getId());
        verify(sessionManager).createRefreshToken(user.getId());

        assertThat(response).isNotNull();
        assertThat(response.getRequest()).isEqualTo(requestToken);
        assertThat(response.getRefresh()).isEqualTo(refreshToken);
    }

    private static SessionToken createSessionToken(String data, String sign) {
        return SessionToken
                .newBuilder()
                .setData(ByteString.copyFromUtf8(data))
                .setSign(ByteString.copyFromUtf8(sign))
                .build();
    }

}
