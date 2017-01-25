package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.errors.CommonServiceErrors
import com.asadmshah.hnclone.errors.PostServiceErrors
import com.asadmshah.hnclone.errors.SessionsServiceErrors
import com.asadmshah.hnclone.errors.UsersServiceErrors
import io.grpc.StatusRuntimeException

internal fun restoreError(e: StatusRuntimeException): StatusRuntimeException {
    val restored = when (e.status.description) {
        UsersServiceErrors.USERNAME_EXISTS.description -> UsersServiceErrors.USERNAME_EXISTS_EXCEPTION
        UsersServiceErrors.NOT_FOUND.description -> UsersServiceErrors.NOT_FOUND_EXCEPTION
        UsersServiceErrors.USERNAME_REQUIRED.description -> UsersServiceErrors.USERNAME_REQUIRED_EXCEPTION
        UsersServiceErrors.USERNAME_INVALID.description -> UsersServiceErrors.USERNAME_INVALID_EXCEPTION
        UsersServiceErrors.PASSWORD_INSECURE.description -> UsersServiceErrors.PASSWORD_INSECURE_EXCEPTION
        UsersServiceErrors.ABOUT_TOO_LONG.description -> UsersServiceErrors.ABOUT_TOO_LONG_EXCEPTION
        SessionsServiceErrors.EXPIRED_TOKEN.description -> SessionsServiceErrors.EXPIRED_TOKEN_EXCEPTION
        SessionsServiceErrors.INVALID_TOKEN.description -> SessionsServiceErrors.INVALID_TOKEN_EXCEPTION
        SessionsServiceErrors.USER_NOT_FOUND.description -> SessionsServiceErrors.USER_NOT_FOUND_EXCEPTION
        PostServiceErrors.TITLE_REQUIRED.description -> PostServiceErrors.TITLE_REQUIRED_EXCEPTION
        PostServiceErrors.TITLE_TOO_LONG.description -> PostServiceErrors.TITLE_TOO_LONG_EXCEPTION
        PostServiceErrors.CONTENT_REQUIRED.description -> PostServiceErrors.CONTENT_REQUIRED_EXCEPTION
        PostServiceErrors.CONTENT_URL_INVALID.description -> PostServiceErrors.CONTENT_URL_INVALID_EXCEPTION
        PostServiceErrors.CONTENT_URL_UNACCEPTABLE.description -> PostServiceErrors.CONTENT_URL_UNACCEPTABLE_EXCEPTION
        PostServiceErrors.CONTENT_TEXT_TOO_LONG.description -> PostServiceErrors.CONTENT_TEXT_TOO_LONG_EXCEPTION
        PostServiceErrors.NOT_FOUND.description -> PostServiceErrors.NOT_FOUND_EXCEPTION
        CommonServiceErrors.UNKNOWN.description -> CommonServiceErrors.UNKNOWN_EXCEPTION
        CommonServiceErrors.UNAUTHENTICATED.description -> CommonServiceErrors.UNAUTHENTICATED_EXCEPTION
        CommonServiceErrors.UNAUTHORIZED.description -> CommonServiceErrors.UNAUTHORIZED_EXCEPTION
        else -> e
    }
    return restored
}