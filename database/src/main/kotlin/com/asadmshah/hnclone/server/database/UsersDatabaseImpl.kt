package com.asadmshah.hnclone.server.database

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
        private val LOG_ROUNDS = 14
    }

    override fun create(username: String, password: String, about: String): User? {
        val hash = hashString(password)

        try {
            return dataSource
                    .executeSingle("SELECT * FROM users_create('$username', '$hash', '$about');", { it.getUser() })
        } catch (e: SQLException) {
            when (e.sqlState) {
                "23505" -> throw UserExistsException()
                else -> throw e
            }
        }
    }

    override fun read(id: Int): User? {
        return dataSource
                .executeSingle("SELECT * FROM users_read($id);", { it.getUser() })
    }

    override fun read(username: String): User? {
        return dataSource
                .executeSingle("SELECT * FROM users_read('$username');", { it.getUser() })
    }

    override fun read(username: String, password: String): User? {
        val good: Boolean? = dataSource
                .executeSingle("SELECT * FROM users_read_password('$username');", {
                    BCrypt.checkpw(password, it.getString(1))
                })

        return if (good ?: false) read(username) else null
    }

    override fun updateAbout(id: Int, about: String): String? {
        return dataSource
                .executeSingle("SELECT * FROM users_update_about($id, '$about');", ResultSet::getString)
    }

    override fun updatePassword(id: Int, password: String): Boolean? {
        val hash = hashString(password)

        return dataSource
                .executeSingle("SELECT * FROM users_update_password($id, '$hash');", { it.getBoolean() })
    }

    override fun delete(id: Int): Boolean? {
        return dataSource
                .executeSingle("SELECT * FROM users_delete($id);", { it.getBoolean() })
    }

    internal fun hashString(i: String): String {
        return BCrypt.hashpw(i, BCrypt.gensalt(LOG_ROUNDS))
    }
}