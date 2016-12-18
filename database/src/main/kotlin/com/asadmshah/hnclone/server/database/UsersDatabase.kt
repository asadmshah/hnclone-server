package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.User
import java.sql.SQLException

/**
 *
 */
interface UsersDatabase {

    /**
     * Creates a user.
     *
     * @param name Unique username.
     * @param pass Password.
     * @param about About. Defaults to empty string.
     *
     * @throws SQLException
     * @throws UserExistsException
     *
     * @return [User] if successful, otherwise null.
     */
    fun create(name: String, pass: String, about: String = ""): User?

    /**
     * @throws SQLException
     */
    fun read(id: Int): User?

    /**
     * @throws SQLException
     */
    fun read(name: String): User?

    /**
     * @throws SQLException
     */
    fun read(name: String, pass: String): User?

    fun update(id: Int, about: String = ""): User?

    fun delete(id: Int): User?
}
