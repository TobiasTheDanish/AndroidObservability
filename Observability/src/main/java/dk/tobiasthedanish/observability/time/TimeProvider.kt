package dk.tobiasthedanish.observability.time

import android.os.SystemClock

internal interface TimeProvider {
    val elapsedRealtime: Long
    fun now(): Long
}

internal class AndroidTimeProvider(): TimeProvider {
    private val startEpoch = System.currentTimeMillis()
    private val startElapsedRealtime = SystemClock.elapsedRealtime()

    override val elapsedRealtime: Long
        get() = SystemClock.elapsedRealtime()

    override fun now(): Long {
        return startEpoch + (SystemClock.elapsedRealtime() - startElapsedRealtime)
    }
}
