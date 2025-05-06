package dk.tobiasthedanish.observability

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class ObservabilityConfig(
    /**
     * Automatically start the SDK after [Observability.init] has been called.
     */
    val autoStart: Boolean = true,
    /**
     * Set the amount of time to wait between data exports.
     * Defaults to 30 seconds
     */
    val timeBetweenExports: Duration = 30.seconds,
    /**
     * Set the max amount of time a session can be active, without crashes and [maxSessionTimeBetweenEvents] being exceeded.
     * Defaults to 6 hours
     */
    val maxSessionDuration: Duration = 6.hours,
    /**
     * Set the max amount of time between events before a session is marked stale, and cannot be reused.
     * Defaults to 20 minutes
     */
    val maxSessionTimeBetweenEvents: Duration = 20.minutes,
)