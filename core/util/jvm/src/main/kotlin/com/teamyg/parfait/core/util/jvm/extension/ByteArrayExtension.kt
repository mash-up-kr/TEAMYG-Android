package com.teamyg.parfait.core.util.jvm.extension

/** 파일 시그니처(매직 넘버)처럼 앞머리 몇 바이트만 보고 판정할 때 쓴다 */
fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { index -> this[index] == prefix[index] }
