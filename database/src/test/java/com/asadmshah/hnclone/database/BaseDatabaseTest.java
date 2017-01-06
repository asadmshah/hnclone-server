package com.asadmshah.hnclone.database;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

class BaseDatabaseTest {

    protected DataSource dataSource;

    protected void init() throws Exception {
        Configuration configuration = new Configurations()
                .properties(UsersDatabaseTest.class.getClassLoader().getResource("test.properties"));

        DatabaseModule module = new DatabaseModule();
        dataSource = module.providesDataSource(configuration);

        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            statement.execute("TRUNCATE sessions RESTART IDENTITY CASCADE;");
            statement.execute("TRUNCATE users RESTART IDENTITY CASCADE;");
            statement.execute("TRUNCATE posts RESTART IDENTITY CASCADE;");
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

}
