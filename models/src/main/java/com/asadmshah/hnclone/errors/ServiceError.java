package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;

import java.nio.ByteBuffer;

public enum ServiceError {

    // IMPORTANT: DO NOT RE-ORDER!
    UNKNOWN,
    UNAUTHENTICATED,
    UNAUTHORIZED,
    COMMENTS_TEXT_REQUIRED,
    COMMENTS_TEXT_TOO_LONG,
    COMMENTS_NOT_FOUND,
    POSTS_TITLE_REQUIRED,
    POSTS_TITLE_TOO_LONG,
    POSTS_CONTENT_REQUIRED,
    POSTS_URL_INVALID,
    POSTS_URL_UNACCEPTABLE,
    POSTS_TEXT_TOO_LONG,
    POSTS_NOT_FOUND,
    SESSIONS_EXPIRED_TOKEN,
    SESSIONS_INVALID_TOKEN,
    USERS_USERNAME_EXISTS,
    USERS_USERNAME_REQUIRED,
    USERS_USERNAME_INVALID,
    USERS_PASSWORD_INSECURE,
    USERS_ABOUT_TOO_LONG,
    USERS_NOT_FOUND
    ;

    static final Metadata.Key<byte[]> KEY = Metadata.Key.of("error-code-bin", Metadata.BINARY_BYTE_MARSHALLER);
    static final ServiceError[] VALUES = ServiceError.values();

    byte[] toCode() {
        return ByteBuffer.allocate(4).putInt(ordinal()).array();
    }

    static ServiceError fromCode(byte[] p) {
        int n = ByteBuffer.wrap(p).getInt();
        if (n < 0 || n >= VALUES.length) {
            return ServiceError.UNKNOWN;
        }
        return VALUES[n];
    }

    public static StatusRuntimeException restore(StatusRuntimeException exception) {
        if (exception.getTrailers() == null || !exception.getTrailers().containsKey(KEY)) {
            return new UnknownStatusException();
        }

        switch (fromCode(exception.getTrailers().get(KEY))) {
            case UNAUTHENTICATED:
                return new UnauthenticatedStatusException();
            case UNAUTHORIZED:
                return new UnauthorizedStatusException();
            case COMMENTS_TEXT_REQUIRED:
                return new CommentTextRequiredStatusException();
            case COMMENTS_TEXT_TOO_LONG:
                return new CommentTextTooLongStatusException();
            case COMMENTS_NOT_FOUND:
                return new CommentNotFoundStatusException();
            case POSTS_TITLE_REQUIRED:
                return new PostTitleRequiredStatusException();
            case POSTS_TITLE_TOO_LONG:
                return new PostTitleTooLongStatusException();
            case POSTS_CONTENT_REQUIRED:
                return new PostContentRequiredStatusException();
            case POSTS_URL_INVALID:
                return new PostURLInvalidStatusException();
            case POSTS_URL_UNACCEPTABLE:
                return new PostURLUnacceptableStatusException();
            case POSTS_TEXT_TOO_LONG:
                return new PostTextTooLongStatusException();
            case POSTS_NOT_FOUND:
                return new PostNotFoundStatusException();
            case SESSIONS_EXPIRED_TOKEN:
                return new SessionExpiredTokenStatusException();
            case SESSIONS_INVALID_TOKEN:
                return new SessionInvalidTokenStatusException();
            case USERS_USERNAME_EXISTS:
                return new UsernameExistsStatusException();
            case USERS_USERNAME_REQUIRED:
                return new UsernameRequiredStatusException();
            case USERS_USERNAME_INVALID:
                return new UsernameInvalidStatusException();
            case USERS_PASSWORD_INSECURE:
                return new PasswordInsecureStatusException();
            case USERS_ABOUT_TOO_LONG:
                return new UserAboutTooLongStatusException();
            case USERS_NOT_FOUND:
                return new UserNotFoundStatusException();
            case UNKNOWN:
            default:
                return new UnknownStatusException();
        }
    }

}
