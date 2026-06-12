package com.teamyg.parfait.core.util.extensions

import java.security.MessageDigest

fun ByteArray.sha256(): String {
    val bytes = MessageDigest
        .getInstance("SHA-256")
        .digest(this)

    return bytes.joinToString(separator = "") { "%02x".format(it) }
}

fun String.sha256(): String = toByteArray().sha256()
