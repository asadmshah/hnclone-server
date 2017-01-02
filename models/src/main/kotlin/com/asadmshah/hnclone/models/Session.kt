package com.asadmshah.hnclone.models

import java.time.LocalDateTime

data class Session(val id: Int,
                   val uuid: String,
                   val expires: LocalDateTime)