package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PostTextTooLongStatusException extends StatusRuntimeException {

    public PostTextTooLongStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Post Text Too Long."), new Metadata());
    }

    private PostTextTooLongStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.POSTS_TEXT_TOO_LONG.toCode());
    }
}
