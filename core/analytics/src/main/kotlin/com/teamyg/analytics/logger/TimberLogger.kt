package com.teamyg.analytics.logger

import android.util.Log
import com.teamyg.analytics.utils.TimberUtils
import timber.log.Timber

class TimberLogger(private val defaultTag: String?) : Logger {
    private inline fun log(
        priority: Int,
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        if (TimberUtils.isDebug.not()) {
            return
        }

        val injectTag = tag ?: defaultTag
        val tree = if (injectTag != null) Timber.tag(injectTag) else Timber

        when (priority) {
            Log.VERBOSE -> tree.v(throwable, message())
            Log.DEBUG -> tree.d(throwable, message())
            Log.INFO -> tree.i(throwable, message())
            Log.WARN -> tree.w(throwable, message())
            Log.ERROR -> tree.e(throwable, message())
            else -> tree.wtf(throwable, message())
        }
    }

    override fun v(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) =
        log(
            priority = Log.VERBOSE,
            tag = tag,
            throwable = throwable,
            message = message,
        )

    override fun d(tag: String?, throwable: Throwable?, message: () -> String) =
        log(
            priority = Log.DEBUG,
            tag = tag,
            throwable = throwable,
            message = message,
        )

    override fun i(tag: String?, throwable: Throwable?, message: () -> String) =
        log(
            priority = Log.INFO,
            tag = tag,
            throwable = throwable,
            message = message,
        )

    override fun w(tag: String?, throwable: Throwable?, message: () -> String) =
        log(
            priority = Log.WARN,
            tag = tag,
            throwable = throwable,
            message = message,
        )

    override fun e(tag: String?, throwable: Throwable?, message: () -> String) =
        log(
            priority = Log.ERROR,
            tag = tag,
            throwable = throwable,
            message = message,
        )

    override fun wtf(tag: String?, throwable: Throwable?, message: () -> String) =
        log(
            priority = Log.ASSERT,
            tag = tag,
            throwable = throwable,
            message = message,
        )
}
