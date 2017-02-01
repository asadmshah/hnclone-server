package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UnknownStatusException extends StatusRuntimeException {

    public UnknownStatusException() {
        this(Status.UNKNOWN.withDescription("An error occurred."));
    }

    private UnknownStatusException(Status status) {
        this(status, new Metadata());
    }

    private UnknownStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.UNKNOWN.toCode());
    }

}
