package com.teamyg.parfait.core.util.jvm.analytics

import co.touchlab.kermit.Logger as KermitLogger

internal class KermitLoggerImpl(
    private val delegate: KermitLogger,
) : Logger {
    override fun v(
        throwable: Throwable?,
        tag: String?,
        message: () -> String,
    ) = delegate.v(
        throwable = throwable,
        tag = tag ?: delegate.tag,
        message = message,
    )

    override fun d(
        throwable: Throwable?,
        tag: String?,
        message: () -> String,
    ) = delegate.d(
        throwable = throwable,
        tag = tag ?: delegate.tag,
        message = message,
    )

    override fun i(
        throwable: Throwable?,
        tag: String?,
        message: () -> String,
    ) = delegate.i(
        throwable = throwable,
        tag = tag ?: delegate.tag,
        message = message,
    )

    override fun w(
        throwable: Throwable?,
        tag: String?,
        message: () -> String,
    ) = delegate.w(
        throwable = throwable,
        tag = tag ?: delegate.tag,
        message = message,
    )

    override fun e(
        throwable: Throwable?,
        tag: String?,
        message: () -> String,
    ) = delegate.e(
        throwable = throwable,
        tag = tag ?: delegate.tag,
        message = message,
    )

    override fun a(
        throwable: Throwable?,
        tag: String?,
        message: () -> String,
    ) = delegate.a(
        throwable = throwable,
        tag = tag ?: delegate.tag,
        message = message,
    )

    internal fun withTag(tag: String): Logger = KermitLoggerImpl(delegate.withTag(tag))
}
