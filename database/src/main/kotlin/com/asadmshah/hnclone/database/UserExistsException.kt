package com.asadmshah.hnclone.database

class UserExistsException internal constructor(message: String = "Username exists.") : RuntimeException(message)