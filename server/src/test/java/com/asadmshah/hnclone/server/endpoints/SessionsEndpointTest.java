package com.asadmshah.hnclone.server.endpoints;

import com.asadmshah.hnclone.common.sessions.SessionManager;
import com.asadmshah.hnclone.models.*;
import com.asadmshah.hnclone.server.ServerComponent;
import com.asadmshah.hnclone.server.database.UsersDatabase;
import com.asadmshah.hnclone.services.SessionsServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SessionsEndpointTest {

    private static final String UNIQUE_SERVER_NAME = "in-process-server for " + SessionsEndpointTest.class.getSimpleName();

    private ManagedChannel inProcessChannel = InProcessChannelBuilder
            .forName(UNIQUE_SERVER_NAME)
            .directExecutor()
            .build();

    private Server inProcessServer;

    @Mock private SessionManager sessionManager;
    @Mock private UsersDatabase usersDatabase;
    @Mock private ServerComponent component;

    @Before
    public void setUp() throws Exception {
        when(component.sessionManager()).thenReturn(sessionManager);
        when(component.usersDatabase()).thenReturn(usersDatabase);

        inProcessServer = InProcessServerBuilder
                .forName(UNIQUE_SERVER_NAME)
                .addService(SessionsEndpoint.create(component))
                .directExecutor()
                .build();
        inProcessServer.start();
    }

    @After
    public void tearDown() {
        inProcessChannel.shutdownNow();
        inProcessServer.shutdownNow();
    }

    @Test
    public void createSessionCorrect() {
        SessionsServiceGrpc.SessionsServiceBlockingStub stub = SessionsServiceGrpc.newBlockingStub(inProcessChannel);

        String expName = "Test Name";
        String expPass = "Test Pass";
        User expUser = createUser(10, expName, expPass, 100);
        String expRequestToken = "Request Token";
        String expRefreshToken = "Refresh Token";

        SessionCreateRequest req = SessionCreateRequest
                .newBuilder()
                .setUsername(expName)
                .setPassword(expPass)
                .build();

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> passCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> reqIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> refIdCaptor = ArgumentCaptor.forClass(Integer.class);

        when(usersDatabase.read(nameCaptor.capture(), passCaptor.capture())).thenReturn(expUser);
        when(sessionManager.createRequestToken(reqIdCaptor.capture(), any(Session.Scopes.class), anyLong(), anyLong())).thenReturn(expRequestToken);
        when(sessionManager.createRefreshToken(refIdCaptor.capture(), any(Session.Scopes.class), anyLong(), anyLong())).thenReturn(expRefreshToken);

        SessionCreateResponse res = stub.create(req);

        assertThat(nameCaptor.getValue()).matches(expName);
        assertThat(passCaptor.getValue()).matches(expPass);
        assertThat(reqIdCaptor.getValue()).isEqualTo(expUser.getId());
        assertThat(refIdCaptor.getValue()).isEqualTo(expUser.getId());

        assertThat(res.getRequest()).matches(expRequestToken);
        assertThat(res.getRefresh()).matches(expRefreshToken);
    }

    @Test
    public void testSessionCorrect() {
        SessionsServiceGrpc.SessionsServiceBlockingStub stub = SessionsServiceGrpc.newBlockingStub(inProcessChannel);

        String expRefToken = "Refresh Token";
        String expReqToken = "Request Token";
        Session expSession = createSession(10);

        SessionRefreshRequest req = SessionRefreshRequest
                .newBuilder()
                .setToken(expRefToken)
                .build();

        ArgumentCaptor<String> refTokenCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> reqIdCaptor = ArgumentCaptor.forClass(Integer.class);

        when(sessionManager.parseRefreshToken(refTokenCapture.capture())).thenReturn(expSession);
        when(sessionManager.createRequestToken(reqIdCaptor.capture(), any(Session.Scopes.class), anyLong(), anyLong())).thenReturn(expReqToken);

        SessionRefreshResponse res = stub.refresh(req);

        assertThat(refTokenCapture.getValue()).matches(expRefToken);
        assertThat(reqIdCaptor.getValue()).isEqualTo(expSession.getId());

        assertThat(res.getToken()).matches(expReqToken);
    }

    static User createUser(int id, String name, String about, int karma) {
        return User
                .newBuilder()
                .setId(id)
                .setName(name)
                .setAbout(about)
                .setKarma(karma)
                .build();
    }

    static Session createSession(int id) {
        long idt = System.currentTimeMillis();
        long edt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
        return createSession(id, Session.Scopes.USER, idt, edt);
    }

    static Session createSession(int id, Session.Scopes scope, long idt, long edt) {
        return Session
                .newBuilder()
                .setId(id)
                .setScope(scope)
                .setIssuedDt(idt)
                .setExpireDt(edt)
                .build();
    }

}
