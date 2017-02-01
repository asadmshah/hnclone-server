package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class CommentTextTooLongStatusException extends StatusRuntimeException {

    public CommentTextTooLongStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Comment Text Too Long."));
    }

    private CommentTextTooLongStatusException(Status status) {
        this(status, new Metadata());
    }

    private CommentTextTooLongStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.COMMENTS_TEXT_TOO_LONG.toCode());
    }
}
