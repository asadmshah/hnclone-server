package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Post
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.inject.Inject
import javax.sql.DataSource

internal class PostsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource): PostsDatabase {

    companion object {
        //language=PostgreSQL
        private const val SQL_CREATE_POST = ""
        //language=PostgreSQL
        private const val SQL_DELETE_POST = ""
        //language=PostgreSQL
        private const val SQL_FLAG_POST = ""
        //language=PostgreSQL
        private const val SQL_READ_POST = ""
        //language=PostgreSQL
        private const val SQL_READ_POSTS_NEW = ""
        //language=PostgreSQL
        private const val SQL_READ_POSTS_HOT = ""
        //language=PostgreSQL
        private const val SQL_READ_POSTS_FROM_USER = ""
        //language=PostgreSQL
        private const val SQL_VOTE_INCREMENT = ""
        //language=PostgreSQL
        private const val SQL_VOTE_DECREMENT = ""
        //language=PostgreSQL
        private const val SQL_VOTE_REMOVE = ""
    }

    override fun create(currUser: Int, title: String, url: String?, text: String?): Post? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var response: Post? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_CREATE_POST)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                // TODO: Handle Result
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return response
    }

    override fun delete(postId: Int): Boolean {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var response = false

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_DELETE_POST)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                // TODO: Handle Result
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return response
    }

    override fun read(currUser: Int, postId: Int): Post? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var response: Post? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_READ_POST)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                // TODO: Handle Result
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return response
    }

    override fun readNew(currUser: Int, lim: Int, off: Int, observer: (Post) -> Unit) {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_READ_POSTS_NEW)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            rslt.let {
                while (it.next()) {
                    // TODO: Emit Posts.
                }
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }
    }

    override fun readHot(currUser: Int, lim: Int, off: Int, observer: (Post) -> Unit) {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_READ_POSTS_HOT)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            rslt.let {
                while (it.next()) {
                    // TODO: Emit Posts.
                }
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }
    }

    override fun readFromUser(currUser: Int, userId: Int, lim: Int, off: Int, observer: (Post) -> Unit) {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_READ_POSTS_FROM_USER)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            rslt.let {
                while (it.next()) {
                    // TODO: Emit Posts.
                }
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }
    }

    override fun voteIncrement(currUser: Int, postId: Int): Int? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var response: Int? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_VOTE_INCREMENT)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                // TODO: Handle Result.
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return response
    }

    override fun voteDecrement(currUser: Int, postId: Int): Int? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var response: Int? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_VOTE_DECREMENT)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                // TODO: Handle Result.
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return response
    }

    override fun voteRemove(currUser: Int, postId: Int): Int? {
        var conn: Connection? = null
        var stmt: PreparedStatement? = null
        var rslt: ResultSet? = null
        var response: Int? = null

        try {
            conn = dataSource.connection

            stmt = conn.prepareStatement(SQL_VOTE_REMOVE)
            // TODO: Prepare Statement

            rslt = stmt.executeQuery()
            if (rslt != null && rslt.next()) {
                // TODO: Handle Result.
            }
        } finally {
            try { rslt?.close() } catch (ignored: Exception) {  }
            try { stmt?.close() } catch (ignored: Exception) {  }
            try { conn?.close() } catch (ignored: Exception) {  }
        }

        return response
    }

}