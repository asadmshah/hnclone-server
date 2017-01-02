package com.asadmshah.hnclone.server.database;

import com.asadmshah.hnclone.models.User;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static com.google.common.truth.Truth.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UsersDatabaseTest {

    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configurations()
                .properties(UsersDatabaseTest.class.getClassLoader().getResource("test.properties"));

        DatabaseModule module = new DatabaseModule();
        dataSource = module.providesDataSource(configuration);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute("TRUNCATE users RESTART IDENTITY CASCADE;");
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    @After
    public void tearDown() throws Exception {
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
