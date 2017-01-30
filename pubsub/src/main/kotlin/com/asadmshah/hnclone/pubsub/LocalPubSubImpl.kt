package com.asadmshah.hnclone.pubsub

import com.asadmshah.hnclone.models.Comment
import com.asadmshah.hnclone.models.CommentScore
import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

internal class LocalPubSubImpl @Inject constructor(): PubSub {

    private val publisherPostScore = PublishProcessor.create<PostScore>()
    private val publisherPost = PublishProcessor.create<Post>()
    private val publisherComments = PublishProcessor.create<Comment>()
    private val publisherCommentScores = PublishProcessor.create<CommentScore>()

    override fun start() {

    }

    override fun stop() {

    }

    override fun pubPostScore(postScore: PostScore) {
        publisherPostScore.onNext(postScore)
    }

    override fun subPostScore(): Flowable<PostScore> {
        return publisherPostScore
    }

    override fun pubPost(post: Post) {
        publisherPost.onNext(post)
    }

    override fun subPost(): Flowable<Post> {
        return publisherPost
    }

    override fun pubComment(comment: Comment) {
        publisherComments.onNext(comment)
    }

    override fun subComments(): Flowable<Comment> {
        return publisherComments
    }

    override fun pubCommentScore(commentScore: CommentScore) {
        publisherCommentScores.onNext(commentScore)
    }

    override fun subCommentScores(): Flowable<CommentScore> {
        return publisherCommentScores
    }
}