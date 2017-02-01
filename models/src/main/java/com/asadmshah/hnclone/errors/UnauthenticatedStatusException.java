package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UnauthenticatedStatusException extends StatusRuntimeException {

    public UnauthenticatedStatusException() {
        this(Status.UNAUTHENTICATED.withDescription("Unauthenticated."));
    }

    private UnauthenticatedStatusException(Status status) {
        this(status, new Metadata());
    }

    private UnauthenticatedStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.UNAUTHENTICATED.toCode());
    }
}
