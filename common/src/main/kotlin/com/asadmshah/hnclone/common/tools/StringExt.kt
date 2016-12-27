package com.asadmshah.hnclone.common.tools

import org.apache.commons.lang3.StringEscapeUtils

fun String.escape(): String {
    return escapeHtml().escapeJs()
}

fun String.escapeHtml(): String {
    return StringEscapeUtils.escapeHtml4(this)
}

fun String.escapeJs(): String {
    return StringEscapeUtils.escapeEcmaScript(this)
}

fun String.unescape(): String {
    return unescapeJs().unescapeHtml()
}

fun String.unescapeHtml(): String {
    return StringEscapeUtils.unescapeHtml4(this)
}

fun String.unescapeJs(): String {
    return StringEscapeUtils.unescapeEcmaScript(this)
}