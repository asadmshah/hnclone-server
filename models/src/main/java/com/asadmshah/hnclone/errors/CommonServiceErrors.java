package com.asadmshah.hnclone.errors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class CommonServiceErrors {

    public static final Status UNKNOWN = Status
            .UNKNOWN
            .withDescription("An error occurred.");

    public static final StatusRuntimeException UNKNOWN_EXCEPTION = UNKNOWN
            .asRuntimeException();

    public static final Status UNAUTHENTICATED = Status
            .UNAUTHENTICATED
            .withDescription("Unauthenticated.");

    public static final StatusRuntimeException UNAUTHENTICATED_EXCEPTION = UNAUTHENTICATED
            .asRuntimeException();

    public static final Status UNAUTHORIZED = Status
            .PERMISSION_DENIED
            .withDescription("Unauthorized.");

    public static final StatusRuntimeException UNAUTHORIZED_EXCEPTION = UNAUTHORIZED
            .asRuntimeException();

    private CommonServiceErrors() {
    }

}
