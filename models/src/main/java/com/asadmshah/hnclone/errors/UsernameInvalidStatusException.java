package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UsernameInvalidStatusException extends StatusRuntimeException {

    public UsernameInvalidStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Username Invalid."), new Metadata());
    }

    private UsernameInvalidStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.USERS_USERNAME_INVALID.toCode());
    }
}
