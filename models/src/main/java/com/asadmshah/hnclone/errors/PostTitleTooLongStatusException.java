package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PostTitleTooLongStatusException extends StatusRuntimeException {

    public PostTitleTooLongStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Post Title Too Long."));
    }

    private PostTitleTooLongStatusException(Status status) {
        this(status, new Metadata());
    }

    private PostTitleTooLongStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.POSTS_TITLE_TOO_LONG.toCode());
    }
}
