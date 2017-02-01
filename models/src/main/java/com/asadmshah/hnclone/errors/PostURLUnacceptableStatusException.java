package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PostURLUnacceptableStatusException extends StatusRuntimeException {

    public PostURLUnacceptableStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Unacceptable Post URL."), new Metadata());
    }

    private PostURLUnacceptableStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.POSTS_URL_UNACCEPTABLE.toCode());
    }
}
