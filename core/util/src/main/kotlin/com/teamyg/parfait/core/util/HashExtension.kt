package com.teamyg.parfait.core.util

import java.security.MessageDigest

fun String.sha256(): String {
    val bytes = MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())

    return bytes.joinToString(separator = "") { "%02x".format(it) }
}
