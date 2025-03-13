package dk.tobiasthedanish.observability.exception

import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.time.TimeProvider
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.atomic.AtomicBoolean

internal class UnhandledExceptionCollector(
    private val timeProvider: TimeProvider,
    private val eventTracker: EventTracker,
): UncaughtExceptionHandler, Collector {
    private var isRegistered = AtomicBoolean(false)
    private var originalUncaughtExceptionHandler: UncaughtExceptionHandler? = null

    override fun register() {
        if (isRegistered.compareAndSet(false, true)) {
            originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(this)
        }
    }

    override fun unregister() {
        if (isRegistered.compareAndSet(true, false)) {
            Thread.setDefaultUncaughtExceptionHandler(originalUncaughtExceptionHandler)
            originalUncaughtExceptionHandler = null
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val event = createExceptionEvent(thread, throwable)

        eventTracker.track(
            data = event,
            timeStamp = timeProvider.now(),
            type = EventTypes.UNHANDLED_EXCEPTION,
        )
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