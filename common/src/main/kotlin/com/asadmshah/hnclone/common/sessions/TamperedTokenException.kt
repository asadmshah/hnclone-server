package com.asadmshah.hnclone.common.sessions

class TamperedTokenException internal constructor() : RuntimeException("Token signature does not match.")