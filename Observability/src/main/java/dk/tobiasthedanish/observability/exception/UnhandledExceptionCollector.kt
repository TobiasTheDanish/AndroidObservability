package dk.tobiasthedanish.observability.exception

import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.events.EventTracker
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.time.TimeProvider
import java.lang.Thread.UncaughtExceptionHandler

internal class UnhandledExceptionCollector(
    private val timeProvider: TimeProvider,
    private val eventTracker: EventTracker,
) : UncaughtExceptionHandler, Collector {
    private var originalUncaughtExceptionHandler: UncaughtExceptionHandler? = null

    override fun register() {
        originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun unregister() {
        Thread.setDefaultUncaughtExceptionHandler(originalUncaughtExceptionHandler)
        originalUncaughtExceptionHandler = null
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val event = ExceptionEventFactory.create(thread, throwable)

            eventTracker.track(
                data = event,
                timeStamp = timeProvider.now(),
                type = EventTypes.EXCEPTION,
            )
        } finally {
            originalUncaughtExceptionHandler?.uncaughtException(thread, throwable)
        }
    }
}