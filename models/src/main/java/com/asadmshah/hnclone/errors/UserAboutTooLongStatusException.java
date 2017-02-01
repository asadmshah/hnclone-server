package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class UserAboutTooLongStatusException extends StatusRuntimeException {

    public UserAboutTooLongStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("About Too Long."), new Metadata());
    }

    private UserAboutTooLongStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.USERS_ABOUT_TOO_LONG.toCode());
    }
}
