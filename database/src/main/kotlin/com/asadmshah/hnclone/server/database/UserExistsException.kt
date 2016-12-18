package com.asadmshah.hnclone.server.database

class UserExistsException internal constructor(message: String = "Username exists.") : RuntimeException(message)