package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PostURLInvalidStatusException extends StatusRuntimeException {

    public PostURLInvalidStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Post URL Invalid."));
    }

    private PostURLInvalidStatusException(Status status) {
        this(status, new Metadata());
    }

    private PostURLInvalidStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.POSTS_URL_INVALID.toCode());
    }

}
