package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class SessionInvalidTokenStatusException extends StatusRuntimeException {

    public SessionInvalidTokenStatusException() {
        this(Status.PERMISSION_DENIED.withDescription("Invalid Token."), new Metadata());
    }

    private SessionInvalidTokenStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.SESSIONS_INVALID_TOKEN.toCode());
    }
}
