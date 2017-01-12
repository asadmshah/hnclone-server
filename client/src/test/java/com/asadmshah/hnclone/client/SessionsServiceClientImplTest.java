package com.asadmshah.hnclone.client;

import com.asadmshah.hnclone.common.sessions.ExpiredTokenException;
import com.asadmshah.hnclone.common.sessions.InvalidTokenException;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.UsersDatabase;
import com.asadmshah.hnclone.errors.SessionsServiceErrors;
import com.asadmshah.hnclone.models.RefreshSession;
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
    @Mock private ServerComponent component;
    @Mock private SessionStorage sessionStorage;

    private BaseClient baseClient;
    private SessionsServiceClientImpl sessionsClient;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);

        baseClient = new TestBaseClientImpl(SessionsServiceEndpoint.create(component));
        sessionsClient = new SessionsServiceClientImpl(sessionStorage, baseClient);
    }

    @After
    public void tearDown() throws Exception {
        baseClient.shutdown();
    }

    @Test
    public void refresh_shouldThrowExpiredTokenException() throws Exception {
        when(sessionStorage.getRefreshKey()).thenReturn(SessionToken.getDefaultInstance());
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenThrow(ExpiredTokenException.class);

        StatusRuntimeException exception = null;
        try {
            sessionsClient.refresh().blockingAwait();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(SessionsServiceErrors.EXPIRED_TOKEN_EXCEPTION);
    }

    @Test
    public void refresh_shouldThrowInvalidTokenException() throws Exception {
        when(sessionStorage.getRefreshKey()).thenReturn(SessionToken.getDefaultInstance());
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

        RefreshSession refreshS = RefreshSession.newBuilder().setId(10).setExpire(expire).build();
        SessionToken refreshT = SessionToken.newBuilder().setData(refreshS.toByteString()).build();

        when(sessionStorage.getRefreshKey()).thenReturn(refreshT);

        sessionsClient.refresh().blockingAwait();

        verify(sessionStorage, times(1)).getRefreshKey();
        verify(sessionManager, times(0)).parseRefreshToken(refreshT);
        verify(sessionStorage, times(0)).putRequestKey(SessionToken.getDefaultInstance());
    }

    @Test
    public void refresh_shouldCompleteWithInteractions() throws Exception {
        long expire = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(8);

        RefreshSession refreshS = RefreshSession.newBuilder().setId(10).setExpire(expire).build();
        SessionToken refreshT = SessionToken.newBuilder().setData(refreshS.toByteString()).build();

        when(sessionStorage.getRefreshKey()).thenReturn(refreshT);
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenReturn(RefreshSession.getDefaultInstance());
        when(sessionManager.createRequestToken(anyInt())).thenReturn(SessionToken.getDefaultInstance());

        sessionsClient.refresh().blockingAwait();

        verify(sessionStorage, times(1)).getRefreshKey();
        verify(sessionManager, times(1)).parseRefreshToken(refreshT);
        verify(sessionStorage, times(1)).putRequestKey(SessionToken.getDefaultInstance());
    }

    @Test
    public void refresh_shouldForce() throws Exception {
        long expire = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);

        RefreshSession refreshS = RefreshSession.newBuilder().setId(10).setExpire(expire).build();
        SessionToken refreshT = SessionToken.newBuilder().setData(refreshS.toByteString()).build();

        when(sessionStorage.getRefreshKey()).thenReturn(refreshT);
        when(sessionManager.parseRefreshToken(any(SessionToken.class))).thenReturn(RefreshSession.getDefaultInstance());
        when(sessionManager.createRequestToken(anyInt())).thenReturn(SessionToken.getDefaultInstance());

        sessionsClient.refresh(true).blockingAwait();

        verify(sessionStorage, times(1)).getRefreshKey();
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
