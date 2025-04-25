package dk.tobiasthedanish.observability.tracing

import dk.tobiasthedanish.observability.time.TimeProvider

class FakeTimeProvider(): TimeProvider {
    private val start = 17000000L
    private val interval = 5L
    private var count = 0L

    override val elapsedRealtime: Long
        get() = count++ * interval

    override fun now(): Long {
        return start + elapsedRealtime
    }

}