package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UsernameExistsStatusException extends StatusRuntimeException {

    public UsernameExistsStatusException() {
        this(Status.ALREADY_EXISTS.withDescription("Username Exists."), new Metadata());
    }

    private UsernameExistsStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.USERS_USERNAME_EXISTS.toCode());
    }
}
