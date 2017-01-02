package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.User
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.time.ZoneOffset
import javax.inject.Inject
import javax.sql.DataSource

internal class UsersDatabaseImpl
@Inject
constructor(private val dataSource: DataSource) : UsersDatabase {

    companion object {
        private val LOG_ROUNDS = 14
    }

    internal fun resultSetToUserFull(resultSet: ResultSet): User {
        return User
                .newBuilder()
                .setId(resultSet.getInt(1))
                .setName(resultSet.getString(2))
                .setDatetime(resultSet.getTimestamp(3).toLocalDateTime().toEpochSecond(ZoneOffset.UTC).toDouble())
                .setKarma(resultSet.getInt(4))
                .setAbout(resultSet.getString(5))
                .build()
    }

    override fun create(username: String, password: String, about: String): User? {
        val hash = hashString(password)

        try {
            return executeSingle("SELECT * FROM users_create('$username', '$hash', '$about');", {
                resultSetToUserFull(it)
            })
        } catch (e: SQLException) {
            when (e.sqlState) {
                "23505"     -> throw UserExistsException()
                else        -> throw e
            }
        }
    }

    override fun read(id: Int): User? {
        return executeSingle("SELECT * FROM users_read($id);", {
            resultSetToUserFull(it)
        })
    }

    override fun read(username: String): User? {
        return executeSingle("SELECT * FROM users_read('$username');", {
            resultSetToUserFull(it)
        })
    }

    override fun read(username: String, password: String): User? {
        val good: Boolean? = executeSingle("SELECT * FROM users_read_password('$username');", {
            BCrypt.checkpw(password, it.getString(1))
        })

        return if (good ?: false) read(username) else null
    }

    override fun updateAbout(id: Int, about: String): String? {
        return executeSingle("SELECT * FROM users_update_about($id, '$about');", {
            it.getString(1)
        })
    }

    override fun updatePassword(id: Int, password: String): Boolean? {
        val hash = hashString(password)

        return executeSingle("SELECT * FROM users_update_password($id, '$hash');", {
            it.getBoolean(1)
        })
    }

    override fun delete(id: Int): Boolean? {
        return executeSingle("SELECT * FROM users_delete($id);", {
            it.getBoolean(1)
        })
    }

    internal fun <T> executeSingle(query: String, function: (rslt: ResultSet) -> T): T? {
        var conn: Connection? = null
        var stmt: Statement? = null
        var rslt: ResultSet? = null
        var response: T? = null

        try {
            conn = dataSource.connection

            stmt = conn.createStatement()

            rslt = stmt.executeQuery(query)
            if (rslt != null && rslt.next()) {
                response = function(rslt)
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return response
    }

    internal fun hashString(i: String): String {
        return BCrypt.hashpw(i, BCrypt.gensalt(LOG_ROUNDS))
    }
}