package com.asadmshah.hnclone.database;

import com.asadmshah.hnclone.models.RefreshSession;
import com.asadmshah.hnclone.models.User;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.time.LocalDateTime;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SessionsDatabaseTest extends BaseDatabaseTest {

    @Before
    public void setUp() throws Exception {
        init();
    }

    @Test
    public void test1() throws Exception {
        UsersDatabase udb = new UsersDatabaseImpl(dataSource);
        SessionsDatabase sdb = new SessionsDatabaseImpl(dataSource);

        User user1 = udb.create("Username 1", "Password 1", "");
        assertThat(user1).isNotNull();
        User user2 = udb.create("Username 2", "Password 2", "");
        assertThat(user2).isNotNull();
        User user3 = udb.create("Username 3", "Password 3", "");
        assertThat(user3).isNotNull();

        RefreshSession session1 = sdb.create(user1.getId(), LocalDateTime.now().plusDays(30));
        assertThat(session1).isNotNull();

        assertThat(sdb.read(session1.getUuid())).isNotNull();

        RefreshSession session2 = sdb.create(user1.getId(), LocalDateTime.now().plusDays(90));
        assertThat(session2).isNotNull();

        RefreshSession session3 = sdb.create(user2.getId(), LocalDateTime.now().plusDays(90));
        assertThat(session3).isNotNull();

        List<RefreshSession> user1Sessions = sdb.read(user1.getId()).toList().blockingGet();
        assertThat(user1Sessions).containsExactly(session1, session2);
        assertThat(user1Sessions).doesNotContain(session3);

        RefreshSession session4 = sdb.create(user3.getId(), LocalDateTime.now().plusDays(30));
        assertThat(session4).isNotNull();

        assertThat(sdb.delete(session3.getUuid())).isTrue();
        assertThat(sdb.read(session3.getUuid())).isNull();

        assertThat(sdb.delete(user1.getId())).isEqualTo(2);
        assertThat(sdb.read(user1.getId()).toList().blockingGet()).isEmpty();

        assertThat(sdb.read(session4.getUuid())).isEqualTo(session4);
    }
}
