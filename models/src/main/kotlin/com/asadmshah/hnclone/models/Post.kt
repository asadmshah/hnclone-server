package com.asadmshah.hnclone.models

import java.time.LocalDateTime

data class Post(val id: Int,
                val datetime: LocalDateTime,
                val title: String,
                val text: String,
                val url: String,
                val score: Int,
                val userId: Int,
                val userName: String,
                val voted: Int)