package com.teamyg.parfait.core.util.jvm.analytics

interface Logger {
    fun v(
        throwable: Throwable? = null,
        tag: String? = null,
        message: () -> String,
    )

    fun d(
        throwable: Throwable? = null,
        tag: String? = null,
        message: () -> String,
    )

    fun i(
        throwable: Throwable? = null,
        tag: String? = null,
        message: () -> String,
    )

    fun w(
        throwable: Throwable? = null,
        tag: String? = null,
        message: () -> String,
    )

    fun e(
        throwable: Throwable? = null,
        tag: String? = null,
        message: () -> String,
    )

    fun a(
        throwable: Throwable? = null,
        tag: String? = null,
        message: () -> String,
    )
}
