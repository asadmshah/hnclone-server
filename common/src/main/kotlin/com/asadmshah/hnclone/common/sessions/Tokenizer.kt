package com.asadmshah.hnclone.common.sessions

import com.asadmshah.hnclone.models.Session
import com.google.protobuf.ByteString

internal interface Tokenizer {

    fun encode(key: ByteArray, session: Session): ByteString

    fun decode(key: ByteArray, token: ByteString): Session
}