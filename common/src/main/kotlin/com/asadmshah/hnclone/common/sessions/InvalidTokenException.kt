package com.asadmshah.hnclone.common.sessions

class InvalidTokenException internal constructor(cause: Throwable?) : RuntimeException("Unable to parse token.", cause)