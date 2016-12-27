package com.asadmshah.hnclone.server.database

import com.asadmshah.hnclone.models.Post

interface PostsDatabase {

    fun create(currUser: Int, title: String, url: String?, text: String?): Post?

    fun delete(postId: Int): Boolean

    fun read(currUser: Int, postId: Int): Post?

    fun readNew(currUser: Int, lim: Int = 20, off: Int = 0, observer: (Post) -> Unit)

    fun readHot(currUser: Int, lim: Int = 20, off: Int = 0, observer: (Post) -> Unit)

    fun readFromUser(currUser: Int, userId: Int, lim: Int = 20, off: Int = 0, observer: (Post) -> Unit)

    fun voteIncrement(currUser: Int, postId: Int): Int?

    fun voteDecrement(currUser: Int, postId: Int): Int?

    fun voteRemove(currUser: Int, postId: Int): Int?
}