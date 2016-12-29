package com.asadmshah.hnclone.common.sessions

class ExpiredTokenException internal constructor(): RuntimeException("Token expiration date has elapsed.")