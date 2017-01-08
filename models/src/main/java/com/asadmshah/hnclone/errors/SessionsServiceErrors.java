package com.asadmshah.hnclone.errors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class SessionsServiceErrors {

    public static final Status EXPIRED_TOKEN = Status
            .PERMISSION_DENIED
            .withDescription("Expired token.");

    public static final StatusRuntimeException EXPIRED_TOKEN_EXCEPTION = EXPIRED_TOKEN
            .asRuntimeException();

    public static final Status INVALID_TOKEN = Status
            .PERMISSION_DENIED
            .withDescription("Invalid token.");

    public static final StatusRuntimeException INVALID_TOKEN_EXCEPTION = INVALID_TOKEN
            .asRuntimeException();

    public static final Status USER_NOT_FOUND = Status
            .NOT_FOUND
            .withDescription("Not found.");

    public static final StatusRuntimeException USER_NOT_FOUND_EXCEPTION = USER_NOT_FOUND
            .asRuntimeException();

    private SessionsServiceErrors() {
    }
}
