package com.asadmshah.hnclone.common.sessions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ExpiredTokenException internal constructor(s: Session): RuntimeException("Using token at ${format()} which expired on ${format(s.expireDt)}") {

    internal companion object {
        @JvmStatic internal fun format(dt: Long): String {
            return format(Instant.ofEpochMilli(dt).atOffset(ZoneOffset.UTC).toLocalDateTime())
        }

        @JvmStatic internal fun format(dt: LocalDateTime = LocalDateTime.now()): String {
            return dt.format(DateTimeFormatter.ISO_DATE_TIME)
        }
    }

}