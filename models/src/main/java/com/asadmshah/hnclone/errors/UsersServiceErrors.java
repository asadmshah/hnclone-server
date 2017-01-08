package com.asadmshah.hnclone.errors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class UsersServiceErrors {

    public static final Status USERNAME_EXISTS = Status
            .ALREADY_EXISTS
            .withDescription("Username exists.");

    public static final StatusRuntimeException USERNAME_EXISTS_EXCEPTION = USERNAME_EXISTS
            .asRuntimeException();

    public static final Status NOT_FOUND = Status
            .NOT_FOUND
            .withDescription("User not found.");

    public static final StatusRuntimeException NOT_FOUND_EXCEPTION = NOT_FOUND
            .asRuntimeException();

    public static final Status USERNAME_REQUIRED = Status
            .INVALID_ARGUMENT
            .withDescription("Username required.");

    public static final StatusRuntimeException USERNAME_REQUIRED_EXCEPTION = USERNAME_REQUIRED
            .asRuntimeException();

    public static final Status USERNAME_INVALID = Status
            .INVALID_ARGUMENT
            .withDescription("Username invalid.");

    public static final StatusRuntimeException USERNAME_INVALID_EXCEPTION = USERNAME_INVALID
            .asRuntimeException();

    public static final Status PASSWORD_INSECURE = Status
            .INVALID_ARGUMENT
            .withDescription("Insecure password.");

    public static final StatusRuntimeException PASSWORD_INSECURE_EXCEPTION = PASSWORD_INSECURE
            .asRuntimeException();

    public static final Status ABOUT_TOO_LONG = Status
            .INVALID_ARGUMENT
            .withDescription("Text entered for about is too long.");

    public static final StatusRuntimeException ABOUT_TOO_LONG_EXCEPTION = ABOUT_TOO_LONG
            .asRuntimeException();

    private UsersServiceErrors() {
    }
}
