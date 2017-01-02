package com.asadmshah.hnclone.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.sql.DataSource

@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun providesDataSource(configuration: Configuration): DataSource {
        val hc = HikariConfig()
        hc.connectionTimeout = TimeUnit.SECONDS.toMillis(30)
        hc.maximumPoolSize = configuration.getInt("database.pool.size")
        hc.dataSourceClassName = "com.impossibl.postgres.jdbc.PGDataSource"
        hc.addDataSourceProperty("user", configuration.getString("database.user"))
        hc.addDataSourceProperty("password", configuration.getString("database.pass"))
        hc.addDataSourceProperty("database", configuration.getString("database.name"))
        hc.addDataSourceProperty("host", configuration.getString("database.host"))
        hc.addDataSourceProperty("port", configuration.getString("database.port"))
        hc.addDataSourceProperty("parsedSqlCacheSize", "256")
        return HikariDataSource(hc)
    }

    @Provides
    @Singleton
    internal fun providesUsers(usersDatabase: UsersDatabaseImpl): UsersDatabase {
        return usersDatabase
    }

    @Provides
    @Singleton
    internal fun providesPosts(postsDatabase: PostsDatabaseImpl): PostsDatabase {
        return postsDatabase
    }

}
