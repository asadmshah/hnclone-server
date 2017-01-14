package com.asadmshah.hnclone.client;

import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.UserExistsException;
import com.asadmshah.hnclone.database.UsersDatabase;
import com.asadmshah.hnclone.errors.CommonServiceErrors;
import com.asadmshah.hnclone.errors.UsersServiceErrors;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.models.SessionToken;
import com.asadmshah.hnclone.models.User;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.endpoints.UsersServiceEndpoint;
import com.asadmshah.hnclone.services.UserCreateRequest;
import com.asadmshah.hnclone.services.UserReadUsingIDRequest;
import com.asadmshah.hnclone.services.UserUpdateAboutRequest;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersServiceClientImplTest {

    @Mock private SessionManager sessionManager;
    @Mock private UsersDatabase usersDatabase;
    @Mock private SessionStorage sessions;
    @Mock private ServerComponent component;

    private BaseClient baseClient;
    private SessionsServiceClient sessionsClient;
    private UsersServiceClientImpl usersClient;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);

        baseClient = new TestBaseClientImpl(UsersServiceEndpoint.create(component));
        sessionsClient = new SessionsServiceClientImpl(sessions, baseClient);
        usersClient = new UsersServiceClientImpl(sessions, baseClient, sessionsClient);
    }

    @After
    public void tearDown() throws Exception {
        baseClient.shutdown();
    }

    @Test
    public void create_shouldComplete() throws Exception {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenReturn(User.getDefaultInstance());

        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

        User user = usersClient.create(request).blockingGet();

        verify(usersDatabase).create(request.getUsername(), request.getPassword(), request.getAbout());

        assertThat(user).isNotNull();
    }

    @Test
    public void create_shouldThrowUsernameRequiredException() throws Exception {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("   ")
                .setPassword("password")
                .build();

        StatusRuntimeException exception = null;
        try {
            usersClient.create(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(UsersServiceErrors.USERNAME_REQUIRED_EXCEPTION);
    }

    @Test
    public void create_shouldThrowUsernameInvalidException() throws Exception {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("123456789012345678901234567890123")
                .setPassword("password")
                .build();

        StatusRuntimeException exception = null;
        try {
            usersClient.create(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(UsersServiceErrors.USERNAME_INVALID_EXCEPTION);
    }

    @Test
    public void create_shouldThrowInvalidPasswordException() throws Exception {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("")
                .build();

        StatusRuntimeException exception = null;
        try {
            usersClient.create(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(UsersServiceErrors.PASSWORD_INSECURE_EXCEPTION);
    }

    @Test
    public void create_shouldThrowAboutTooLongException() throws Exception {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .setAbout(StringUtils.repeat("A", 513))
                .build();

        StatusRuntimeException exception = null;
        try {
            usersClient.create(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(UsersServiceErrors.ABOUT_TOO_LONG_EXCEPTION);
    }

    @Test
    public void create_shouldThrowUserExistsException() throws Exception {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenThrow(UserExistsException.class);

        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

        StatusRuntimeException exception = null;
        try {
            usersClient.create(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(UsersServiceErrors.USERNAME_EXISTS_EXCEPTION);
    }

    @Test
    public void create_shouldThrowUnknownException() throws Exception {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenThrow(SQLException.class);

        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

        StatusRuntimeException exception = null;
        try {
            usersClient.create(request).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(CommonServiceErrors.UNKNOWN_EXCEPTION);
    }

    @Test
    public void readUsingID_shouldComplete() throws Exception {
        User expUser = User.newBuilder().setId(10).build();

        when(usersDatabase.read(anyInt())).thenReturn(expUser);

        UserReadUsingIDRequest request = UserReadUsingIDRequest
                .newBuilder()
                .setId(expUser.getId())
                .build();

        User resUser = usersClient.read(request).blockingGet();

        verify(usersDatabase).read(expUser.getId());

        assertThat(resUser).isNotNull();
        assertThat(resUser).isEqualTo(expUser);
    }

    @Test
    public void readUsingID_shouldThrowUnknownException() throws Exception {
        when(usersDatabase.read(anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            usersClient.read(UserReadUsingIDRequest.getDefaultInstance()).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(CommonServiceErrors.UNKNOWN_EXCEPTION);
    }

    @Test
    public void readUsingID_shouldThrowNotFoundException() throws Exception {
        when(usersDatabase.read(anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            usersClient.read(UserReadUsingIDRequest.getDefaultInstance()).blockingGet();
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception).isEqualTo(UsersServiceErrors.NOT_FOUND_EXCEPTION);
    }

    @Test
    public void updateAbout_shouldComplete() throws Exception {
        String updatedAbout = "Updated About";

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(usersDatabase.updateAbout(anyInt(), anyString())).thenReturn(updatedAbout);
        when(sessions.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        UserUpdateAboutRequest request = UserUpdateAboutRequest
                .newBuilder()
                .setAbout(updatedAbout)
                .build();

        String response = usersClient.update(request).blockingGet();

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(request.getAbout());
    }

}
