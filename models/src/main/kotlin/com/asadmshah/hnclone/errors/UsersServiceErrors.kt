package com.asadmshah.hnclone.errors

import io.grpc.Status
import io.grpc.StatusRuntimeException

object UsersServiceErrors {

    val Exists: Status = Status
            .ALREADY_EXISTS
            .withDescription("User exists.")

    val ExistsException: StatusRuntimeException = Exists
            .asRuntimeException()

    val NotFound: Status = Status
            .NOT_FOUND
            .withDescription("User not found.")

    val NotFoundException: StatusRuntimeException = NotFound
            .asRuntimeException()

}