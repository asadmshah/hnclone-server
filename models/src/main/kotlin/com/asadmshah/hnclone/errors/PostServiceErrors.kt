package com.asadmshah.hnclone.errors

import io.grpc.Status
import io.grpc.StatusRuntimeException

object PostServiceErrors {

    val TitleRequired: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Title required.")

    val TitleRequiredException: StatusRuntimeException = TitleRequired
            .asRuntimeException()

    val TitleTooLong: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Title for post is too long.")

    val TitleTooLongException: StatusRuntimeException = TitleTooLong
            .asRuntimeException()

    val ContentRequired: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Content required.")

    val ContentRequiredException: StatusRuntimeException = ContentRequired
            .asRuntimeException()

    val ContentURLInvalid: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Content URL is invalid.")

    val ContentURLInvalidException: StatusRuntimeException = ContentURLInvalid
            .asRuntimeException()

    val ContentURLUnacceptable: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Unacceptable URL.")

    val ContentURLUnacceptableException: StatusRuntimeException = ContentURLUnacceptable
            .asRuntimeException()

    val ContentTextTooLong: Status = Status
            .INVALID_ARGUMENT
            .withDescription("Content Text is too long.")

    val ContentTextTooLongException: StatusRuntimeException = ContentTextTooLong
            .asRuntimeException()

    val NotFound: Status = Status
            .NOT_FOUND
            .withDescription("Post not found.")

    val NotFoundException: StatusRuntimeException = NotFound
            .asRuntimeException()

}