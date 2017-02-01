package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class CommentNotFoundStatusException extends StatusRuntimeException {

    public CommentNotFoundStatusException() {
        this(Status.NOT_FOUND.withDescription("Comment Not Found."));
    }

    private CommentNotFoundStatusException(Status status) {
        this(status, new Metadata());
    }

    private CommentNotFoundStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.COMMENTS_NOT_FOUND.toCode());
    }
}
