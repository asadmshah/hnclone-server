package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.cache.BlockedSessionsCache;
import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.database.UserExistsException;
import com.asadmshah.hnclone.database.UsersDatabase;
import com.asadmshah.hnclone.errors.CommonServiceErrors;
import com.asadmshah.hnclone.errors.UsersServiceErrors;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.models.User;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor;
import com.asadmshah.hnclone.services.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersServiceEndpointTest {

    private static final String SERVER_NAME = "in-process-server for " + UsersServiceEndpointTest.class.getSimpleName();

    private ManagedChannel inProcessChannel = InProcessChannelBuilder
            .forName(SERVER_NAME)
            .directExecutor()
            .build();

    private Server inProcessServer;

    private UsersServiceGrpc.UsersServiceBlockingStub inProcessStub;

    @Mock private SessionManager sessionManager;
    @Mock private UsersDatabase usersDatabase;
    @Mock private BlockedSessionsCache blockedSessionsCache;
    @Mock private ServerComponent component;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);
        when(component.blockedSessionsCache()).thenReturn(blockedSessionsCache);

        when(blockedSessionsCache.contains(anyInt(), any(LocalDateTime.class))).thenReturn(false);

        inProcessServer = InProcessServerBuilder
                .forName(SERVER_NAME)
                .addService(UsersServiceEndpoint.create(component))
                .directExecutor()
                .build();

        inProcessServer.start();

        inProcessStub = UsersServiceGrpc.newBlockingStub(inProcessChannel);
    }

    @After
    public void tearDown() throws Exception {
        inProcessChannel.shutdownNow();
        inProcessServer.shutdownNow();
    }

    @Test
    public void create_shouldThrowUsernameRequiredOnEmptyUsernameException() {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("")
                .setPassword("Test")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.USERNAME_REQUIRED.getDescription());
    }

    @Test
    public void create_shouldThrowUsernameInvalidExceptionOnTooLongUsername() {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername(StringUtils.repeat("T", 33))
                .setPassword("Test")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.USERNAME_INVALID.getDescription());
    }

    @Test
    public void create_shouldThrowUsernameInvalidExceptionOnBadUsername() {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("hunter_$@#")
                .setPassword("Test")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.USERNAME_INVALID.getDescription());
    }

    @Test
    public void create_shouldEscapeInputs() {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("hunter_123")
                .setPassword("password")
                .setAbout("'; DELETE FROM users WHERE 1=1; <script>alert(\"Hello world\");</script>")
                .build();

        String expName = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(request.getUsername()));
        String expPass = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(request.getPassword()));
        String expAbout = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(request.getAbout()));

        User expUser = User
                .newBuilder()
                .setUsername(expName)
                .setAbout(expAbout)
                .build();

        when(usersDatabase.create(anyString(), anyString(), anyString())).thenReturn(expUser);

        User resUser = inProcessStub.create(request);

        verify(usersDatabase).create(expName, expPass, expAbout);

        assertThat(resUser).isEqualTo(expUser);
    }

    @Test
    public void create_shouldThrowInvalidPasswordException() {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("testuser")
                .setPassword("")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.PASSWORD_INSECURE.getDescription());
    }

    @Test
    public void create_shouldThrowAboutTooLongException() {
        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setAbout(StringUtils.repeat("A", 513))
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.ABOUT_TOO_LONG.getDescription());
    }

    @Test
    public void create_shouldThrowUserExistsException() {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenThrow(UserExistsException.class);

        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.create(request);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.USERNAME_EXISTS.getDescription());
    }

    @Test
    public void create_shouldThrowSQLException() {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenThrow(SQLException.class);

        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

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
    public void create_shouldThrowUnknownException() {
        when(usersDatabase.create(anyString(), anyString(), anyString())).thenReturn(null);

        UserCreateRequest request = UserCreateRequest
                .newBuilder()
                .setUsername("username")
                .setPassword("password")
                .build();

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
        String expName = "hunter_123";
        String expPass = "password";
        String expAbout = "Text About";

        User expUser = User
                .newBuilder()
                .setUsername(expName)
                .setAbout(expAbout)
                .build();

        UserCreateRequest req = UserCreateRequest
                .newBuilder()
                .setUsername(expName)
                .setPassword(expPass)
                .setAbout(expAbout)
                .build();

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aboutCaptor = ArgumentCaptor.forClass(String.class);

        when(usersDatabase.create(nameCaptor.capture(), passCaptor.capture(), aboutCaptor.capture())).thenReturn(expUser);

        User resUser = inProcessStub.create(req);

        assertThat(resUser).isNotNull();
        assertThat(resUser).isEqualTo(expUser);

        assertThat(nameCaptor.getValue()).matches(expName);
        assertThat(passCaptor.getValue()).matches(expPass);
        assertThat(aboutCaptor.getValue()).matches(expAbout);
    }

    @Test
    public void readUsingID_shouldThrowSQLException() {
        when(usersDatabase.read(anyInt())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readUsingID(UserReadUsingIDRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNKNOWN.getDescription());
    }

    @Test
    public void readUsingID_shouldThrowNotFoundException() {
        when(usersDatabase.read(anyInt())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readUsingID(UserReadUsingIDRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.NOT_FOUND.getDescription());
    }

    @Test
    public void readUsingID_shouldComplete() {
        User exp = User
                .newBuilder()
                .setId(10)
                .build();

        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);

        when(usersDatabase.read(idCaptor.capture())).thenReturn(exp);

        UserReadUsingIDRequest req = UserReadUsingIDRequest
                .newBuilder()
                .setId(exp.getId())
                .build();

        User res = inProcessStub.readUsingID(req);

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(exp.getId());

        assertThat(idCaptor.getValue()).isEqualTo(exp.getId());
    }

    @Test
    public void readUsingName_shouldThrowSQLException() {
        when(usersDatabase.read(anyString())).thenThrow(SQLException.class);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readUsingName(UserReadUsingNameRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNKNOWN.getDescription());
    }

    @Test
    public void readUsingName_shouldThrowNotFoundException() {
        when(usersDatabase.read(anyString())).thenReturn(null);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.readUsingName(UserReadUsingNameRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.NOT_FOUND.getDescription());
    }

    @Test
    public void readUsingName_shouldComplete() {
        User exp = User
                .newBuilder()
                .setUsername("Test User")
                .build();

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        when(usersDatabase.read(nameCaptor.capture())).thenReturn(exp);

        UserReadUsingNameRequest req = UserReadUsingNameRequest
                .newBuilder()
                .setUsername(exp.getUsername())
                .build();

        User res = inProcessStub.readUsingName(req);

        assertThat(res).isNotNull();
        assertThat(res.getUsername()).isEqualTo(exp.getUsername());

        assertThat(nameCaptor.getValue()).matches(exp.getUsername());
    }

    @Test
    public void updateAbout_shouldThrowUnauthenticatedException() {
        StatusRuntimeException exception = null;
        try {
            inProcessStub.updateAbout(UserUpdateAboutRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNAUTHENTICATED.getDescription());
    }

    @Test
    public void updateAbout_shouldThrowUnknownException() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(usersDatabase.updateAbout(anyInt(), anyString())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.updateAbout(UserUpdateAboutRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNKNOWN.getDescription());
    }

    @Test
    public void updateAbout_shouldThrowNotFoundException() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(usersDatabase.updateAbout(anyInt(), anyString())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.updateAbout(UserUpdateAboutRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNKNOWN.getDescription());
    }

    @Test
    public void updateAbout_shouldThrowTooLongException() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        UserUpdateAboutRequest req = UserUpdateAboutRequest
                .newBuilder()
                .setAbout(StringUtils.repeat("A", 513))
                .build();

        StatusRuntimeException exception = null;
        try {
            inProcessStub.updateAbout(req);
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(UsersServiceErrors.ABOUT_TOO_LONG.getDescription());
    }

    @Test
    public void updateAbout_shouldComplete() {
        User expUser = User
                .newBuilder()
                .setId(10)
                .setAbout("'; DELETE FROM users WHERE 1=1; <script>alert(\"Hello world\");</script>")
                .build();

        String expAbout = StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(expUser.getAbout()));

        RequestSession expSession = RequestSession
                .newBuilder()
                .setId(expUser.getId())
                .build();

        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> aboutCaptor = ArgumentCaptor.forClass(String.class);

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(expSession);
        when(usersDatabase.updateAbout(anyInt(), anyString())).thenReturn(expUser.getAbout());

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        UserUpdateAboutRequest req = UserUpdateAboutRequest
                .newBuilder()
                .setAbout(expUser.getAbout())
                .build();

        UserUpdateAboutResponse resResponse = inProcessStub.updateAbout(req);

        verify(usersDatabase).updateAbout(idCaptor.capture(), aboutCaptor.capture());

        assertThat(idCaptor.getValue()).isEqualTo(expUser.getId());
        assertThat(aboutCaptor.getValue()).isEqualTo(expAbout);

        assertThat(resResponse).isNotNull();
        assertThat(resResponse.getAbout()).isEqualTo(expUser.getAbout());
    }

    @Test
    public void delete_shouldThrowUnauthenticatedException() {
        StatusRuntimeException exception = null;
        try {
            inProcessStub.delete(UserDeleteRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNAUTHENTICATED.getDescription());
    }

    @Test
    public void delete_shouldThrowUnknownException() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(usersDatabase.delete(anyInt())).thenThrow(SQLException.class);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.delete(UserDeleteRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNKNOWN.getDescription());
    }

    @Test
    public void delete_notFoundException() {
        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(RequestSession.getDefaultInstance());
        when(usersDatabase.delete(anyInt())).thenReturn(null);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        StatusRuntimeException exception = null;
        try {
            inProcessStub.delete(UserDeleteRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
        assertThat(exception.getStatus().getDescription()).isEqualTo(CommonServiceErrors.UNKNOWN.getDescription());
    }

    @Test
    public void delete_shouldComplete() {
        User expUser = User
                .newBuilder()
                .setId(10)
                .build();

        RequestSession session = RequestSession
                .newBuilder()
                .setId(expUser.getId())
                .build();

        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(session);
        when(usersDatabase.delete(anyInt())).thenReturn(true);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        UserDeleteRequest req = UserDeleteRequest.getDefaultInstance();

        UserDeleteResponse res = inProcessStub.delete(req);

        verify(usersDatabase).delete(idCaptor.capture());
        verify(blockedSessionsCache).put(expUser.getId());

        assertThat(idCaptor.getValue()).isEqualTo(expUser.getId());

        assertThat(res).isNotNull();
        assertThat(res.getDeleted()).isTrue();
    }

}
