package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UsernameRequiredStatusException extends StatusRuntimeException {

    public UsernameRequiredStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Username Required."), new Metadata());
    }

    private UsernameRequiredStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.USERS_USERNAME_REQUIRED.toCode());
    }
}
