package com.teamyg.analytics

import com.teamyg.analytics.logger.Logger

abstract class AnalyticsHelper(
    private val logger: Logger,
) : Logger {
    override fun v(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        logger.v(
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun d(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        logger.d(
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun i(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        logger.i(
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun w(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        logger.w(
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun e(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        logger.e(
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun wtf(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        logger.wtf(
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }
}
