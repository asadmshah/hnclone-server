package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PostContentRequiredStatusException extends StatusRuntimeException {

    public PostContentRequiredStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Post Text or URL required."));
    }

    private PostContentRequiredStatusException(Status status) {
        this(status, new Metadata());
    }

    private PostContentRequiredStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.POSTS_CONTENT_REQUIRED.toCode());
    }
}
