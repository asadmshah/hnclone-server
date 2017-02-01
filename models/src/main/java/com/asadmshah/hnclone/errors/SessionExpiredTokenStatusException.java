package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class SessionExpiredTokenStatusException extends StatusRuntimeException {

    public SessionExpiredTokenStatusException() {
        this(Status.PERMISSION_DENIED.withDescription("Expired Token."), new Metadata());
    }

    private SessionExpiredTokenStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.SESSIONS_EXPIRED_TOKEN.toCode());
    }
}
