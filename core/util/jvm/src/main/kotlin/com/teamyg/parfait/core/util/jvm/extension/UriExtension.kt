package com.teamyg.parfait.core.util.jvm.extension

import java.net.URI

/** `android.net.Uri` 가 아니라 `java.net.URI` 다 — 순수 JVM 단위 테스트에서도 돈다. 형식 오류면 `null` 이다. */
fun String.toJavaUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()
