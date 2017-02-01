package com.asadmshah.hnclone.errors;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nonnull;

public final class PostTitleRequiredStatusException extends StatusRuntimeException {

    public PostTitleRequiredStatusException() {
        this(Status.INVALID_ARGUMENT.withDescription("Post Title Required."));
    }

    private PostTitleRequiredStatusException(Status status) {
        this(status, new Metadata());
    }

    private PostTitleRequiredStatusException(Status status, @Nonnull Metadata trailers) {
        super(status, trailers);
        trailers.put(ServiceError.KEY, ServiceError.POSTS_TITLE_REQUIRED.toCode());
    }
}
