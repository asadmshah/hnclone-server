package com.asadmshah.hnclone.pubsub

import com.asadmshah.hnclone.models.Comment
import com.asadmshah.hnclone.models.CommentScore
import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.models.PostScore
import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.RedisURI
import com.lambdaworks.redis.api.StatefulRedisConnection
import com.lambdaworks.redis.api.sync.RedisCommands
import com.lambdaworks.redis.codec.ByteArrayCodec
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection
import com.lambdaworks.redis.pubsub.api.rx.RedisPubSubReactiveCommands
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import org.apache.commons.configuration2.Configuration
import rx.Subscriber
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

internal class RedisPubSubImpl
@Inject
constructor(private val configuration: Configuration) : PubSub {

    private val publisherPostScore = PublishProcessor.create<PostScore>()
    private val publisherPost = PublishProcessor.create<Post>()
    private val publisherComments = PublishProcessor.create<Comment>()
    private val publisherCommentScores = PublishProcessor.create<CommentScore>()
    private val subscriptions = CompositeSubscription()

    private var client: RedisClient? = null
    private var connBasic: StatefulRedisConnection<ByteArray, ByteArray>? = null
    private var commBasic: RedisCommands<ByteArray, ByteArray>? = null

    private var connSub: StatefulRedisPubSubConnection<ByteArray, ByteArray>? = null
    private var commSub: RedisPubSubReactiveCommands<ByteArray, ByteArray>? = null

    override fun start() {
        val uri = RedisURI.Builder
                .redis(configuration.getString("redis.host", "localhost"), configuration.getInt("redis.port", 6379))
                .withDatabase(configuration.getInt("redis.db", 0))
                .withPassword(configuration.getString("redis.pass", ""))
                .build()

        client = RedisClient.create(uri)

        connBasic = client?.connect(ByteArrayCodec())
        commBasic = connBasic?.sync()

        connSub = client?.connectPubSub(ByteArrayCodec())
        commSub = connSub?.reactive()

        subscriptions.add(observe(Channels.POST_SCORE, PostScore::parseFrom, publisherPostScore))
        subscriptions.add(observe(Channels.POST, Post::parseFrom, publisherPost))
        subscriptions.add(observe(Channels.COMMENTS, Comment::parseFrom, publisherComments))
        subscriptions.add(observe(Channels.COMMENT_SCORES, CommentScore::parseFrom, publisherCommentScores))
    }

    override fun stop() {
        if (!subscriptions.isUnsubscribed) subscriptions.unsubscribe()

        commSub?.close()
        connSub?.close()

        commBasic?.close()
        connBasic?.close()

        client?.shutdown()
    }

    override fun pubPostScore(postScore: PostScore) {
        commBasic?.publish(Channels.POST_SCORE, postScore.toByteArray())
    }

    override fun subPostScore(): Flowable<PostScore> {
        return publisherPostScore
    }

    internal fun <T> observe(channel: ByteArray, mapper: (ByteArray) -> T, processor: PublishProcessor<T>): Subscription {
        if (commSub == null) throw RuntimeException("Redis client not connected.")

        commSub!!.subscribe(channel).toBlocking().first()
        return commSub!!
                .observeChannels()
                .map { it.message }
                .map { mapper(it) }
                .subscribe(object : Subscriber<T>() {
                    override fun onCompleted() {
                        processor.onComplete()
                    }

                    override fun onError(e: Throwable) {
                        processor.onError(e)
                    }

                    override fun onNext(t: T) {
                        processor.onNext(t)
                    }
                })
    }

    override fun pubPost(post: Post) {
        commBasic?.publish(Channels.POST, post.toByteArray())
    }

    override fun subPost(): Flowable<Post> {
        return publisherPost
    }

    override fun pubComment(comment: Comment) {
        commBasic?.publish(Channels.COMMENTS, comment.toByteArray())
    }

    override fun subComments(): Flowable<Comment> {
        return publisherComments
    }

    override fun pubCommentScore(commentScore: CommentScore) {
        commBasic?.publish(Channels.COMMENT_SCORES, commentScore.toByteArray())
    }

    override fun subCommentScores(): Flowable<CommentScore> {
        return publisherCommentScores
    }
}