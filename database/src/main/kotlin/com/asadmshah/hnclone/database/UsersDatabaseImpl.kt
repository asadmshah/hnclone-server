package com.asadmshah.hnclone.database

import com.asadmshah.hnclone.models.User
import org.mindrot.jbcrypt.BCrypt
import java.sql.ResultSet
import java.sql.SQLException
import javax.inject.Inject
import javax.sql.DataSource

internal class UsersDatabaseImpl
@Inject
constructor(private val dataSource: DataSource) : UsersDatabase {

    companion object {
        // language=PostgreSQL
        private const val SQL_CREATE = "SELECT * FROM users_create(?, ?, ?);"
        // language=PostgreSQL
        private const val SQL_READ_ID = "SELECT * FROM users_read_id(?);"
        // language=PostgreSQL
        private const val SQL_READ_USERNAME = "SELECT * FROM users_read_username(?);"
        // language=PostgreSQL
        private const val SQL_READ_PASSWORD = "SELECT * FROM users_read_password(?);"
        // language=PostgreSQL
        private const val SQL_UPDATE_ABOUT = "SELECT * FROM users_update_about(?, ?);"
        // language=PostgreSQL
        private const val SQL_UPDATE_PASSWORD = "SELECT * FROM users_update_password(?, ?);"

        internal val LOG_ROUNDS = 4
    }

    override fun create(username: String, password: String, about: String): User? {
        try {
            return dataSource
                    .executeSingle(SQL_CREATE, {
                        it.setString(1, username)
                        it.setString(2, hashString(password))
                        it.setString(3, about)
                    }, ResultSet::getUser)
        } catch (e: SQLException) {
            when (e.sqlState) {
                "23505" -> throw UserExistsException()
                else -> throw e
            }
        }
    }

    override fun read(id: Int): User? {
        return dataSource
                .executeSingle(SQL_READ_ID, {
                    it.setInt(1, id)
                }, ResultSet::getUser)
    }

    override fun read(username: String): User? {
        return dataSource
                .executeSingle(SQL_READ_USERNAME, {
                    it.setString(1, username)
                }, ResultSet::getUser)
    }

    override fun read(username: String, password: String): User? {
        val hash = dataSource
                .executeSingle(SQL_READ_PASSWORD, {
                    it.setString(1, username)
                }, ResultSet::getString) ?: ""

        return if (BCrypt.checkpw(password, hash)) read(username) else null
    }

    override fun updateAbout(id: Int, about: String): String? {
        return dataSource
                .executeSingle(SQL_UPDATE_ABOUT, {
                    it.setInt(1, id)
                    it.setString(2, about)
                }, ResultSet::getString)
    }

    override fun updatePassword(id: Int, password: String): Boolean? {
        return dataSource
                .executeSingle(SQL_UPDATE_PASSWORD, {
                    it.setInt(1, id)
                    it.setString(2, hashString(password))
                }, ResultSet::getBoolean)
    }

    internal fun hashString(i: String): String {
        return BCrypt.hashpw(i, BCrypt.gensalt(LOG_ROUNDS))
    }
}