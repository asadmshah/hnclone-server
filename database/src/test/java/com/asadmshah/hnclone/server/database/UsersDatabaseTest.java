package com.asadmshah.hnclone.server.database;

import com.asadmshah.hnclone.models.User;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static com.google.common.truth.Truth.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersDatabaseTest extends BaseDatabaseTest {

    @Before
    public void setUp() throws Exception {
        init();
    }

    @Test
    public void test1() throws Exception {
        UsersDatabase db = new UsersDatabaseImpl(dataSource);

        User user1 = db.create("Username 1", "Password 1", "About 1");
        assertThat(user1).isNotNull();
        User user2 = db.create("Username 2", "Password 2", "About 2");
        assertThat(user2).isNotNull();
        User user3 = db.create("Username 3", "Password 3", "About 3");
        assertThat(user3).isNotNull();

        User user1Res = db.read(1);
        assertThat(user1Res).isNotNull();
        assertThat(user1Res).isEqualTo(user1);

        assertThat(db.read(-1)).isNull();

        User user2Res = db.read("Username 2");
        assertThat(user2Res).isNotNull();
        assertThat(user2Res).isEqualTo(user2);

        assertThat(db.read("Nonexistent")).isNull();

        User user3Res = db.read("Username 3", "Password 3");
        assertThat(user3Res).isNotNull();
        assertThat(user3Res).isEqualTo(user3);

        assertThat(db.read("Username 3", "Incorrect")).isNull();

        assertThat(db.updateAbout(1, "Updated About 1")).isEqualTo("Updated About 1");
        assertThat(db.read(1).getAbout()).isEqualTo("Updated About 1");

        assertThat(db.updatePassword(1, "Updated Password 1")).isTrue();
        assertThat(db.read("Username 1", "Updated Password 1")).isNotNull();

        assertThat(db.delete(1)).isTrue();
        assertThat(db.read(1)).isNull();
        assertThat(db.updateAbout(1, "")).isNull();
    }
}
