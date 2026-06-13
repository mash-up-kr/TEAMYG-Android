package com.teamyg.parfait.core.util.extensions

import java.security.MessageDigest

fun ByteArray.sha256(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this)
    .joinToString(separator = "") { "%02x".format(it) }

fun String.sha256(): String = toByteArray().sha256()
