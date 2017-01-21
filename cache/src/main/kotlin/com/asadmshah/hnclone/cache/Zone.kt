package com.asadmshah.hnclone.cache

enum class Zone(private val k: String) {

    BLOCKED_SESSIONS("blockedsessions:");

    internal fun key(s: String): String {
        return k.plus(s)
    }

}