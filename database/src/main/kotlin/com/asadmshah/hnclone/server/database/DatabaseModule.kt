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
        hc.username = configuration.getString("database.user")
        hc.password = configuration.getString("database.pass")
        hc.connectionTimeout = TimeUnit.SECONDS.toMillis(30)
        hc.maximumPoolSize = configuration.getInt("database.pool.size")
        hc.dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
        hc.addDataSourceProperty("databaseName", configuration.getString("database.name"))
        hc.addDataSourceProperty("serverName", configuration.getString("database.host"))
        hc.addDataSourceProperty("portNumber", configuration.getString("database.port"))
        hc.addDataSourceProperty("preparedStatementCacheQueries", "256")
        return HikariDataSource(hc)
    }

    @Provides
    @Singleton
    internal fun providesUsers(usersDatabase: UsersDatabaseImpl): UsersDatabase {
        return usersDatabase
    }

}
