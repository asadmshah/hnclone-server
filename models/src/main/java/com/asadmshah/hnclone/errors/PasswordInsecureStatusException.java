package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PasswordInsecureStatusException extends StatusRuntimeException {

    public PasswordInsecureStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Password Insecure."), new Metadata());
    }

    private PasswordInsecureStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.USERS_PASSWORD_INSECURE.toCode());
    }
}
