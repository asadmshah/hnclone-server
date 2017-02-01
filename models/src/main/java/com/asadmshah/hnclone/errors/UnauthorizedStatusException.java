package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UnauthorizedStatusException extends StatusRuntimeException {

    public UnauthorizedStatusException() {
        this(Status.PERMISSION_DENIED.withDescription("Unauthorized."));
    }

    private UnauthorizedStatusException(Status status) {
        this(status, new Metadata());
    }

    private UnauthorizedStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.UNAUTHORIZED.toCode());
    }
}
