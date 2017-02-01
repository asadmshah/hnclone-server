package com.asadmshah.hnclone.client;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.UserExistsException;
import com.asadmshah.hnclone.database.UsersDatabase;
import com.asadmshah.hnclone.errors.*;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.models.SessionToken;
import com.asadmshah.hnclone.models.User;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.endpoints.UsersServiceEndpoint;
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
    @Mock private BlockedSessionsCache blockedSessionsCache;

    private BaseClient baseClient;
    private SessionsServiceClient sessionsClient;
    private UsersServiceClientImpl usersClient;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);

        baseClient = TestBaseClient.create(UsersServiceEndpoint.create(component));
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

        User user = usersClient.create("username", "password", "").blockingGet();

        verify(usersDatabase).create("username", "password", "");

        assertThat(user).isNotNull();
    }

    @Test(expected = UsernameRequiredStatusException.class)
    public void create_shouldThrowUsernameRequiredException() throws Exception {
        usersClient.create("   ", "password", "").blockingGet();
    }

    @Test(expected = UsernameInvalidStatusException.class)
    public void create_shouldThrowUsernameInvalidException() throws Exception {
        usersClient.create("123456789012345678901234567890123", "password", "").blockingGet();
    }

    @Test(expected = PasswordInsecureStatusException.class)
    public void create_shouldThrowInvalidPasswordException() throws Exception {
        usersClient.create("username", "", "").blockingGet();
    }

    @Test(expected = UserAboutTooLongStatusException.class)
    public void create_shouldThrowAboutTooLongException() throws Exception {
        usersClient.create("username", "password", StringUtils.repeat("A", 513)).blockingGet();
    }

    @Test(expected = UsernameExistsStatusException.class)
    public void create_shouldThrowUserExistsException() throws Exception {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenThrow(UserExistsException.class);

        usersClient.create("username", "password", "").blockingGet();
    }

    @Test(expected = UnknownStatusException.class)
    public void create_shouldThrowUnknownException() throws Exception {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenThrow(SQLException.class);

        usersClient.create("username", "password", "").blockingGet();
    }

    @Test
    public void readUsingID_shouldComplete() throws Exception {
        User expUser = User.newBuilder().setId(10).build();

        when(usersDatabase.read(anyInt())).thenReturn(expUser);

        User resUser = usersClient.read(expUser.getId()).blockingGet();

        verify(usersDatabase).read(expUser.getId());

        assertThat(resUser).isNotNull();
        assertThat(resUser).isEqualTo(expUser);
    }

    @Test(expected = UnknownStatusException.class)
    public void readUsingID_shouldThrowUnknownException() throws Exception {
        when(usersDatabase.read(anyInt())).thenThrow(SQLException.class);

        usersClient.read(1).blockingGet();
    }

    @Test(expected = UserNotFoundStatusException.class)
    public void readUsingID_shouldThrowNotFoundException() throws Exception {
        when(usersDatabase.read(anyInt())).thenReturn(null);

        usersClient.read(1).blockingGet();
    }

    @Test
    public void updateAbout_shouldComplete() throws Exception {
        String updatedAbout = "Updated About";

        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(usersDatabase.updateAbout(anyInt(), anyString())).thenReturn(updatedAbout);
        when(sessions.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);

        String response = usersClient.updateAbout(updatedAbout).blockingGet();

        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(updatedAbout);
    }

    @Test
    public void updatePassword_shouldComplete() throws Exception {
        RequestSession requestS = RequestSession.newBuilder().setId(10).setExpire(System.currentTimeMillis() + 60_000).build();
        SessionToken requestT = SessionToken.newBuilder().setData(requestS.toByteString()).build();

        when(sessions.getRequestKey()).thenReturn(requestT);
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(requestS);
        when(usersDatabase.updatePassword(anyInt(), anyString())).thenReturn(true);

        usersClient.updatePassword("password").blockingAwait();
    }

}
