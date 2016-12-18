package com.asadmshah.hnclone.server.database

import org.mindrot.jbcrypt.BCrypt
import java.sql.*
import javax.inject.Inject
import javax.sql.DataSource

internal class UsersDatabaseImpl
@Inject
constructor(private val dataSource: DataSource) : UsersDatabase {

    companion object {
        //language=PostgreSQL
        private val SQL_CREATE_USER = "SELECT * FROM users_create_user(?, ?, ?);"
        //language=PostgreSQL
        private val SQL_READ_USER_USING_ID = "SELECT * FROM users_read_user(?);"
        //language=PostgreSQL
        private val SQL_READ_USER_USING_NAME = "SELECT * FROM users_read_user(?);"
        //language=PostgreSQL
        private val SQL_READ_USER_PASS = "SELECT users_read_user_pass(?);"
        //language=PostgreSQL
        private val SQL_UPDATE_ABOUT = "SELECT * FROM users_update_user_about(?, ?);"
        //language=PostgreSQL
        private val SQL_DELETE = "SELECT * FROM users_delete_user(?);"
    }

    internal fun resultSetToUserFull(resultSet: ResultSet): User {
        return User
                .newBuilder()
                .setId(resultSet.getInt(1))
                .setName(resultSet.getString(2))
                .setDatetime(resultSet.getDouble(3))
                .setKarma(resultSet.getInt(4))
                .setAbout(resultSet.getString(5))
                .build()
    }

    override fun create(name: String, pass: String, about: String): User? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var user: User? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_CREATE_USER)
            stmt.setString(1, name)
            stmt.setString(2, BCrypt.hashpw(pass, BCrypt.gensalt()))
            stmt.setString(3, about)

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                user = resultSetToUserFull(rslt)
            }
        } catch (e: SQLException) {
            when (e.sqlState) {
                "23505"     -> throw UserExistsException()
                else        -> throw e
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return user
    }

    override fun read(id: Int): User? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var user: User? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_READ_USER_USING_ID)
            stmt.setInt(1, id)

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                user = resultSetToUserFull(rslt)
            }
        } catch (e: SQLException) {
            throw e
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return user
    }

    override fun read(name: String): User? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var user: User? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_READ_USER_USING_NAME)
            stmt.setString(1, name)

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                user = resultSetToUserFull(rslt)
            }
        } catch (e: SQLException) {
            throw e
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return user
    }

    override fun read(name: String, pass: String): User? {
        var conn: Connection? = null
        var stmt: CallableStatement? = null
        var rslt: ResultSet? = null
        var good: Boolean = false

        try {
            conn = dataSource.connection

            stmt = conn.prepareCall(SQL_READ_USER_PASS)
            stmt.setString(1, name)

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                good = BCrypt.checkpw(pass, rslt.getString(1))
            }
        } catch (e: SQLException) {
            throw e
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return if (good) read(name) else null
    }

}