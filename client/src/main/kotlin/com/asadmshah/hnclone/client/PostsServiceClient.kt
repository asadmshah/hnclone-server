package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import com.asadmshah.hnclone.services.PostScoreResponse
import io.reactivex.Flowable
import io.reactivex.Single

interface PostsServiceClient {

    fun create(title: String, text: String?, url: String?): Single<Post>

    fun read(id: Int): Single<Post>

    fun readNewStream(lim: Int, off: Int): Flowable<Post>

    fun readNewStream(userId: Int, lim: Int, off: Int): Flowable<Post>

    fun readHotStream(lim: Int, off: Int): Flowable<Post>

    fun readHotStream(userId: Int, lim: Int, off: Int): Flowable<Post>

    fun voteIncrement(id: Int): Single<PostScoreResponse>

    fun voteDecrement(id: Int): Single<PostScoreResponse>

    fun voteRemove(id: Int): Single<PostScoreResponse>

    fun subscribeToPostScoresStream(): Flowable<PostScore>

    fun subscribeToPostScoresStream(id: Int): Flowable<PostScore>

}