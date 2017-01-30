package com.asadmshah.hnclone.errors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class CommentsServiceErrors {

    public static final Status TEXT_REQUIRED = Status
            .INVALID_ARGUMENT
            .withDescription("Comment Text Required.");

    public static final StatusRuntimeException TEXT_REQUIRED_EXCEPTION = TEXT_REQUIRED
            .asRuntimeException();

    public static final Status TEXT_TOO_LONG = Status
            .INVALID_ARGUMENT
            .withDescription("Text Too Long.");

    public static final StatusRuntimeException TEXT_TOO_LONG_EXCEPTION = TEXT_TOO_LONG
            .asRuntimeException();

    private CommentsServiceErrors() {
    }
}
