package com.asadmshah.hnclone.models

import java.time.LocalDateTime

data class User(val id: Int,
                val username: String,
                val created: LocalDateTime,
                val score: Int,
                val about: String)