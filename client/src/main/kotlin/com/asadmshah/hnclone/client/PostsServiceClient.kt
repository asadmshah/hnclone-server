package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.Post
import com.asadmshah.hnclone.services.*
import io.reactivex.Flowable
import io.reactivex.Single

interface PostsServiceClient {

    fun create(request: PostCreateRequest): Single<Post>

    fun delete(request: PostDeleteRequest): Single<Int>

    fun read(request: PostReadRequest): Single<Post>

    fun readNew(request: PostReadListRequest): Flowable<Post>

    fun readNew(request: PostReadListFromUserRequest): Flowable<Post>

    fun readHot(request: PostReadListRequest): Flowable<Post>

    fun readHot(request: PostReadListFromUserRequest): Flowable<Post>

    fun vote(request: PostVoteIncrementRequest): Single<PostScoreResponse>

    fun vote(request: PostVoteDecrementRequest): Single<PostScoreResponse>

    fun vote(request: PostVoteRemoveRequest): Single<PostScoreResponse>

}