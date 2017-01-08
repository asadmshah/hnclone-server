package com.asadmshah.hnclone.errors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class PostServiceErrors {

    public static final Status TITLE_REQUIRED = Status
            .INVALID_ARGUMENT
            .withDescription("Title required.");

    public static final StatusRuntimeException TITLE_REQUIRED_EXCEPTION = TITLE_REQUIRED
            .asRuntimeException();

    public static final Status TITLE_TOO_LONG = Status
            .INVALID_ARGUMENT
            .withDescription("Title for post is too long.");

    public static final StatusRuntimeException TITLE_TOO_LONG_EXCEPTION = TITLE_TOO_LONG
            .asRuntimeException();

    public static final Status CONTENT_REQUIRED = Status
            .INVALID_ARGUMENT
            .withDescription("Content required.");

    public static final StatusRuntimeException CONTENT_REQUIRED_EXCEPTION = CONTENT_REQUIRED
            .asRuntimeException();

    public static final Status CONTENT_URL_INVALID = Status
            .INVALID_ARGUMENT
            .withDescription("Invalid URL.");

    public static final StatusRuntimeException CONTENT_URL_INVALID_EXCEPTION = CONTENT_URL_INVALID
            .asRuntimeException();

    public static final Status CONTENT_URL_UNACCEPTABLE = Status
            .INVALID_ARGUMENT
            .withDescription("Unacceptable URL.");

    public static final StatusRuntimeException CONTENT_URL_UNACCEPTABLE_EXCEPTION = CONTENT_URL_UNACCEPTABLE
            .asRuntimeException();

    public static final Status CONTENT_TEXT_TOO_LONG = Status
            .INVALID_ARGUMENT
            .withDescription("Text is too long.");

    public static final StatusRuntimeException CONTENT_TEXT_TOO_LONG_EXCEPTION = CONTENT_TEXT_TOO_LONG
            .asRuntimeException();

    public static final Status NOT_FOUND = Status
            .INVALID_ARGUMENT
            .withDescription("Post not found.");

    public static final StatusRuntimeException NOT_FOUND_EXCEPTION = NOT_FOUND
            .asRuntimeException();

    private PostServiceErrors() {
    }
}
