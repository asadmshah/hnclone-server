package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class CommentTextRequiredStatusException extends StatusRuntimeException {

    public CommentTextRequiredStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Comment Text Required."));
    }

    private CommentTextRequiredStatusException(Status status) {
        this(status, new Metadata());
    }

    private CommentTextRequiredStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.COMMENTS_TEXT_REQUIRED.toCode());
    }
}
