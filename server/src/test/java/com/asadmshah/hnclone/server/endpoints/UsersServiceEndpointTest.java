package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.models.*;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.database.UsersDatabase;
import com.asadmshah.hnclone.server.interceptors.SessionInterceptor;
import com.asadmshah.hnclone.services.UsersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    @Mock private ServerComponent component;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);

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
    public void createUserCorrect() {
        String expName = "Test Name";
        String expPass = "Text Pass";
        String expAbout = "Text About";

        User expUser = User
                .newBuilder()
                .setName(expName)
                .setAbout(expAbout)
                .build();

        UserCreateRequest req = UserCreateRequest
                .newBuilder()
                .setName(expName)
                .setPass(expPass)
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
    public void readUsingIDCorrect() {
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
    public void readUsingNameCorrect() {
        User exp = User
                .newBuilder()
                .setName("Test User")
                .build();

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        when(usersDatabase.read(nameCaptor.capture())).thenReturn(exp);

        UserReadUsingNameRequest req = UserReadUsingNameRequest
                .newBuilder()
                .setName(exp.getName())
                .build();

        User res = inProcessStub.readUsingName(req);

        assertThat(res).isNotNull();
        assertThat(res.getName()).isEqualTo(exp.getName());

        assertThat(nameCaptor.getValue()).matches(exp.getName());
    }

    @Test
    public void updateAboutCorrect() {
        User expUser = User
                .newBuilder()
                .setId(10)
                .setAbout("Test About")
                .build();

        RequestSession expSession = RequestSession
                .newBuilder()
                .setId(expUser.getId())
                .build();

        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> aboutCaptor = ArgumentCaptor.forClass(String.class);

        when(sessionManager.parseRequestToken(any(byte[].class))).thenReturn(expSession);
        when(usersDatabase.update(anyInt(), anyString())).thenReturn(expUser);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        UserUpdateAboutRequest req = UserUpdateAboutRequest
                .newBuilder()
                .setId(10)
                .setAbout(expUser.getAbout())
                .build();

        User resUser = inProcessStub.updateAbout(req);

        verify(usersDatabase).update(idCaptor.capture(), aboutCaptor.capture());

        assertThat(idCaptor.getValue()).isEqualTo(expUser.getId());
        assertThat(aboutCaptor.getValue()).isEqualTo(expUser.getAbout());

        assertThat(resUser).isNotNull();
        assertThat(resUser).isEqualTo(expUser);
    }

    @Test
    public void deleteCorrect() {
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
        when(usersDatabase.delete(anyInt())).thenReturn(expUser);

        Metadata metadata = new Metadata();
        metadata.put(SessionInterceptor.Companion.getHEADER_KEY(), " ".getBytes());
        inProcessStub = MetadataUtils.attachHeaders(inProcessStub, metadata);

        UserDeleteRequest req = UserDeleteRequest
                .newBuilder()
                .setId(expUser.getId())
                .build();

        User resUser = inProcessStub.delete(req);

        verify(usersDatabase).delete(idCaptor.capture());

        assertThat(idCaptor.getValue()).isEqualTo(expUser.getId());

        assertThat(resUser).isNotNull();
        assertThat(resUser).isEqualTo(expUser);
    }

}
