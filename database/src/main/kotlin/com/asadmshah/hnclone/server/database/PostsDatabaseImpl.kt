package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Post
import javax.inject.Inject
import javax.sql.DataSource

internal class PostsDatabaseImpl
@Inject
constructor(private val dataSource: DataSource): PostsDatabase {

    override fun create(currUser: Int, title: String, url: String?, text: String?): Post? {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(postId: Int): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun read(currUser: Int, postId: Int): Post? {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readNew(currUser: Int, lim: Int, off: Int, observer: (Post) -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readHot(currUser: Int, lim: Int, off: Int, observer: (Post) -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readFromUser(currUser: Int, userId: Int, lim: Int, off: Int, observer: (Post) -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun voteIncrement(currUser: Int, postId: Int): Int? {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun voteDecrement(currUser: Int, postId: Int): Int? {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun voteRemove(currUser: Int, postId: Int): Int? {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}