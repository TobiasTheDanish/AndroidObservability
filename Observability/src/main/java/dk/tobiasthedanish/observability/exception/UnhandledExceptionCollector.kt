package dk.tobiasthedanish.observability.exception

import android.util.Log
import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.time.TimeProvider
import java.lang.Thread.UncaughtExceptionHandler

internal class UnhandledExceptionCollector(
    private val timeProvider: TimeProvider,
    private val eventTracker: EventTracker,
): UncaughtExceptionHandler, Collector {
    private var originalUncaughtExceptionHandler: UncaughtExceptionHandler? = null

    override fun register() {
        Log.d("UnhandledExceptionCollector", "Register")

        originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun unregister() {
        Log.d("UnhandledExceptionCollector", "Unregister")

        Thread.setDefaultUncaughtExceptionHandler(originalUncaughtExceptionHandler)
        originalUncaughtExceptionHandler = null
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.d("UnhandledExceptionCollector", "UncaughtException")
        try {
            val event = createExceptionEvent(thread, throwable)

            eventTracker.track(
                data = event,
                timeStamp = timeProvider.now(),
                type = EventTypes.UNHANDLED_EXCEPTION,
            )

        } finally {
            originalUncaughtExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createExceptionEvent(thread: Thread, throwable: Throwable, handled: Boolean = false): ExceptionEvent {
        val exceptionUnits = mutableListOf<ExceptionUnit>()

        var error: Throwable? = throwable
        while (error != null) {
            val frames = error.stackTrace.trim().map {
                StackFrameUnit(
                    className = it.className,
                    fileName = it.fileName,
                    methodName = it.methodName,
                    lineNum = it.lineNumber,
                )
            }
            exceptionUnits.add(
                ExceptionUnit(
                frames = frames,
                message = error.message,
                name = error.javaClass.name,
            )
            )

            error = error.cause
        }

        return ExceptionEvent(
            threadName = thread.name,
            exceptionUnits = exceptionUnits,
            handled = handled,
        )
    }
}