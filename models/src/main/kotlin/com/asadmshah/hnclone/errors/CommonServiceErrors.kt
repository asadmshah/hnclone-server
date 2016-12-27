package com.asadmshah.hnclone.errors

import io.grpc.Status
import io.grpc.StatusRuntimeException

object CommonServiceErrors {

    val Unknown: Status = Status
            .INTERNAL
            .withDescription("An error occurred.")

    val UnknownException: StatusRuntimeException = Unknown
            .asRuntimeException()

    val Unauthenticated: Status = Status
            .UNAUTHENTICATED
            .withDescription("Unauthenticated.")

    val UnauthenticatedException: StatusRuntimeException = Unauthenticated
            .asRuntimeException()

    val Unauthorized: Status = Status
            .PERMISSION_DENIED
            .withDescription("Permission Denied.")

    val UnauthorizedException: StatusRuntimeException = Unauthorized
            .asRuntimeException()

}