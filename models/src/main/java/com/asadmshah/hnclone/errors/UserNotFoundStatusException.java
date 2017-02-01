package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UserNotFoundStatusException extends StatusRuntimeException {

    public UserNotFoundStatusException() {
        this(Status.NOT_FOUND.withDescription("User Not Found."), new Metadata());
    }

    private UserNotFoundStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.USERS_NOT_FOUND.toCode());
    }
}
