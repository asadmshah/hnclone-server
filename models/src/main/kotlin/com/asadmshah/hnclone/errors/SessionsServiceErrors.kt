package com.asadmshah.hnclone.errors

import io.grpc.Status
import io.grpc.StatusRuntimeException

object SessionsServiceErrors {

    val ExpiredToken: Status = Status
            .PERMISSION_DENIED
            .withDescription("Expired Token.")

    val ExpiredTokenException: StatusRuntimeException = ExpiredToken
            .asRuntimeException()

    val InvalidToken: Status = Status
            .PERMISSION_DENIED
            .withDescription("Invalid Token.")

    val InvalidTokenException: StatusRuntimeException = InvalidToken
            .asRuntimeException()

    val UserNotFound: Status = Status
            .NOT_FOUND
            .withDescription("User not found.")

    val UserNotFoundException: StatusRuntimeException = UserNotFound
            .asRuntimeException()
}