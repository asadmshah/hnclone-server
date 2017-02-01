package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PostNotFoundStatusException extends StatusRuntimeException {

    public PostNotFoundStatusException() {
        this(Status.NOT_FOUND.withDescription("Post Not Found."), new Metadata());
    }

    private PostNotFoundStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.POSTS_NOT_FOUND.toCode());
    }
}
