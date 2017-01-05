package com.asadmshah.hnclone.common.sessions;

import com.asadmshah.hnclone.models.RefreshSession;
import com.asadmshah.hnclone.models.RequestSession;
import com.asadmshah.hnclone.models.SessionToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.truth.Truth.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SessionManagerImplTest {

    private static final byte[] REQ_KEY = "test_req".getBytes();
    private static final byte[] REF_KEY = "test_ref".getBytes();

    private SessionManager sm;

    @Before
    public void setUp() throws Exception {
        sm = new SessionManagerImpl(REQ_KEY, REF_KEY);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void requestToken_shouldComplete() throws Exception {
        for (int i = 0; i < 10000; i++) {
            int a = ThreadLocalRandom.current().nextInt();
            long b = System.currentTimeMillis();
            long c = System.currentTimeMillis() + 1000;

            RequestSession exp = requestSession(a, b, c);
            RequestSession res = sm.parseRequestToken(sm.createRequestToken(exp).toByteArray());

            assertThat(res).isEqualTo(exp);
        }
    }

    @Test
    public void requestToken_shouldThrowTamperedTokenException() throws Exception {
        long idt = System.currentTimeMillis();
        long edt = System.currentTimeMillis() + 1000;

        RequestSession exp = requestSession(10, idt, edt);
        SessionToken token = sm.createRequestToken(exp);
        RequestSession res = RequestSession.parseFrom(token.getData()).toBuilder()
                .setId(20)
                .build();
        token = token.toBuilder().setData(res.toByteString()).build();

        RuntimeException exception = null;
        try {
            sm.parseRequestToken(token.toByteArray());
        } catch (TamperedTokenException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
    }

    @Test
    public void requestToken_shouldThrowExpiredTokenException() throws Exception {
        long idt = System.currentTimeMillis();
        long edt = System.currentTimeMillis() - 1000;

        RequestSession exp = requestSession(10, idt, edt);
        SessionToken tok = sm.createRequestToken(exp);

        RuntimeException exception = null;
        try {
            sm.parseRequestToken(tok.toByteArray());
        } catch (ExpiredTokenException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
    }

    @Test
    public void refreshToken_shouldComplete() throws Exception {
        for (int i = 0; i < 10000; i++) {
            int a = ThreadLocalRandom.current().nextInt();
            long b = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt();
            long c = System.currentTimeMillis() + 1000;

            RefreshSession exp = refreshSession(a, b, c);
            RefreshSession res = sm.parseRefreshToken(sm.createRefreshToken(exp).toByteArray());

            assertThat(res).isEqualTo(exp);
        }
    }

    @Test
    public void refreshToken_shouldThrowTamperedTokenException() throws Exception {
        long idt = System.currentTimeMillis();
        long edt = System.currentTimeMillis() + 1000;

        RefreshSession exp = refreshSession(10, idt, edt);
        SessionToken token = sm.createRefreshToken(exp);
        RefreshSession res = RefreshSession.parseFrom(token.getData()).toBuilder()
                .setId(20)
                .build();
        token = token.toBuilder().setData(res.toByteString()).build();

        RuntimeException exception = null;
        try {
            sm.parseRefreshToken(token.toByteArray());
        } catch (TamperedTokenException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
    }

    @Test
    public void refreshToken_shouldThrowExpiredTokenException() throws Exception {
        long idt = System.currentTimeMillis();
        long edt = System.currentTimeMillis() - 1000;

        RefreshSession exp = refreshSession(10, idt, edt);
        SessionToken tok = sm.createRefreshToken(exp);

        RuntimeException exception = null;
        try {
            sm.parseRefreshToken(tok.toByteArray());
        } catch (ExpiredTokenException e) {
            exception = e;
        }

        assertThat(exception).isNotNull();
    }

    private static RequestSession requestSession(int id, long idt, long edt) {
        return RequestSession
                .newBuilder()
                .setId(id)
                .setIssued(idt)
                .setExpire(edt)
                .build();
    }

    private static RefreshSession refreshSession(int id, long idt, long edt) {
        return RefreshSession
                .newBuilder()
                .setId(id)
                .setIssued(idt)
                .setExpire(edt)
                .build();
    }
}
