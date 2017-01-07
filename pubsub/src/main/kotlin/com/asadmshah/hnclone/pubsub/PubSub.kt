package com.asadmshah.hnclone.pubsub

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import io.reactivex.Flowable

interface PubSub {

    fun start()

    fun stop()

    fun pubPostScore(postScore: PostScore)

    fun subPostScore(): Flowable<PostScore>

    fun pubPost(post: Post)

    fun subPost(): Flowable<Post>
}