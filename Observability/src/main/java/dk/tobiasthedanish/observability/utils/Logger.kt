package dk.tobiasthedanish.observability.utils

import android.util.Log


internal class Logger(private val tag: String) {
    fun verbose(msg: String, throwable: Throwable? = null) {
        Log.v(tag, msg, throwable)
    }

    fun debug(msg: String, throwable: Throwable? = null) {
        Log.d(tag, msg, throwable)
    }

    fun info(msg: String, throwable: Throwable? = null) {
        Log.i(tag, msg, throwable)
    }

    fun warning(msg: String, throwable: Throwable? = null) {
        Log.w(tag, msg, throwable)
    }

    fun error(msg: String, throwable: Throwable? = null) {
        Log.e(tag, msg, throwable)
    }
}