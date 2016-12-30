package com.asadmshah.hnclone.errors

import io.grpc.Status
import io.grpc.StatusRuntimeException

object UsersServiceErrors {

    val Exists: Status = Status
            .ALREADY_EXISTS
            .withDescription("Username exists.")

    val ExistsException: StatusRuntimeException = Exists
            .asRuntimeException()

    val NotFound: Status = Status
            .NOT_FOUND
            .withDescription("User not found.")

    val NotFoundException: StatusRuntimeException = NotFound
            .asRuntimeException()

    val UsernameRequired: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Username required.")

    val UsernameRequiredException: StatusRuntimeException = UsernameRequired
            .asRuntimeException()

    val UsernameInvalid: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Unable to use this username.")

    val UsernameInvalidException: StatusRuntimeException = UsernameInvalid
            .asRuntimeException()

    val PasswordInsecure: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Please use a more secure password.")

    val PasswordInsecureException: StatusRuntimeException = PasswordInsecure
            .asRuntimeException()

    val AboutTooLong: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Unable to use text for about.")

    val AboutTooLongException: StatusRuntimeException = AboutTooLong
            .asRuntimeException()
}