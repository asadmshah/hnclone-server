package com.asadmshah.hnclone.client;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.InvalidTokenException;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.SessionsDatabase;
import com.asadmshah.hnclone.database.UsersDatabase;
import com.asadmshah.hnclone.errors.SessionsServiceErrors;
import com.asadmshah.hnclone.models.RefreshSession;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.models.SessionToken;
import com.asadmshah.hnclone.models.User;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.endpoints.SessionsServiceEndpoint;
import com.asadmshah.hnclone.services.SessionCreateRequest;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SessionsServiceClientImplTest {

    @Mock private SessionManager sessionManager;
    @Mock private UsersDatabase usersDatabase;
    @Mock private SessionsDatabase sessionsDatabase;
    @Mock private ServerComponent component;
    @Mock private SessionStorage sessionStorage;
    @Mock private BlockedSessionsCache blockedSessionsCache;

    private BaseClient baseClient;
    private SessionsServiceClientImpl sessionsClient;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);
        when(component.sessionsDatabase()).thenReturn(sessionsDatabase);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);

        baseClient = new TestBaseClientImpl(SessionsServiceEndpoint.create(component));
        sessionsClient = new SessionsServiceClientImpl(sessionStorage, baseClient);
    }

    @After
    public void tearDown() throws Exception {
        baseClient.shutdown();
    }

    @Test
    public void refresh_shouldThrowExpiredTokenException() throws Exception {
        when(sessionStorage.getRequestKey()).thenReturn(SessionToken.getDefaultInstance());
        when(sessionStorage.getRefreshKey()).thenReturn(SessionToken.getDefaultInstance());
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenThrow(SessionsServiceErrors.EXPIRED_TOKEN_EXCEPTION);

        StatusRuntimeException exception = null;
        try {
            sessionsClient.refresh(true).blockingAwait();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(SessionsServiceErrors.EXPIRED_TOKEN_EXCEPTION);
    }

    @Test
    public void refresh_shouldThrowInvalidTokenException() throws Exception {
        when(sessionStorage.getRequestKey()).thenReturn(SessionToken.getDefaultInstance());
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenThrow(InvalidTokenException.class);

        StatusRuntimeException exception = null;
        try {
            sessionsClient.refresh().blockingAwait();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(SessionsServiceErrors.INVALID_TOKEN_EXCEPTION);
    }

    @Test
    public void refresh_shouldCompleteWithNoInteractions() throws Exception {
        long expire = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(expire).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionsDatabase.read(anyString())).thenReturn(RefreshSession.getDefaultInstance());

        sessionsClient.refresh().blockingAwait();

        verify(sessionStorage, times(1)).getRequestKey();
        verify(sessionManager, times(0)).parseRefreshToken(requestT);
        verify(sessionStorage, times(0)).putRequestKey(SessionToken.getDefaultInstance());
    }

    @Test
    public void refresh_shouldCompleteWithInteractions() throws Exception {
        long expire = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(8);

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(expire).build();
        RefreshSession refreshS = RefreshSession.newBuilder().setId(10).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();
        SessionToken refreshT = SessionToken.newBuilder().setData(refreshS.toByteString()).build();

        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionStorage.getRefreshKey()).thenReturn(refreshT);
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenReturn(RefreshSession.getDefaultInstance());
        when(sessionManager.createRequestToken(anyInt())).thenReturn(SessionToken.getDefaultInstance());
        when(sessionsDatabase.read(anyString())).thenReturn(RefreshSession.getDefaultInstance());

        sessionsClient.refresh().blockingAwait();

        verify(sessionStorage, times(1)).getRequestKey();
        verify(sessionStorage, times(1)).getRefreshKey();
        verify(sessionManager, times(1)).parseRefreshToken(refreshT);
        verify(sessionStorage, times(1)).putRequestKey(SessionToken.getDefaultInstance());
    }

    @Test
    public void refresh_shouldForce() throws Exception {
        long expire = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(expire).build();
        RefreshSession refreshS = RefreshSession.newBuilder().setId(10).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();
        SessionToken refreshT = SessionToken.newBuilder().setData(refreshS.toByteString()).build();

        when(sessionStorage.getRequestKey()).thenReturn(requestT);
        when(sessionStorage.getRefreshKey()).thenReturn(refreshT);
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenReturn(RefreshSession.getDefaultInstance());
        when(sessionManager.createRequestToken(anyInt())).thenReturn(SessionToken.getDefaultInstance());
        when(sessionsDatabase.read(anyString())).thenReturn(RefreshSession.getDefaultInstance());

        sessionsClient.refresh(true).blockingAwait();

        verify(sessionStorage, times(1)).getRefreshKey();
        verify(sessionStorage, times(1)).getRequestKey();
        verify(sessionManager, times(1)).parseRefreshToken(refreshT);
        verify(sessionStorage, times(1)).putRequestKey(SessionToken.getDefaultInstance());
    }

    @Test
    public void create_shouldThrowUserNotFoundException() throws Exception {
        when(usersDatabase.read(anyString(), anyString())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            sessionsClient.create(SessionCreateRequest.getDefaultInstance()).blockingAwait();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(SessionsServiceErrors.USER_NOT_FOUND_EXCEPTION);
    }

    @Test
    public void create_shouldComplete() throws Exception {
        SessionToken ref = SessionToken.newBuilder().setData(ByteString.copyFrom("ref".getBytes())).build();
        SessionToken req = SessionToken.newBuilder().setData(ByteString.copyFrom("req".getBytes())).build();

        when(usersDatabase.read(anyString(), anyString())).thenReturn(User.getDefaultInstance());
        when(sessionManager.createRequestToken(anyInt())).thenReturn(req);
        when(sessionManager.createRefreshToken(anyInt())).thenReturn(ref);

        SessionCreateRequest request = SessionCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

        sessionsClient.create(request).blockingAwait();

        verify(usersDatabase).read(request.getUsername(), request.getPassword());
        verify(sessionStorage).putRequestKey(req);
        verify(sessionStorage).putRefreshKey(ref);
    }
}
