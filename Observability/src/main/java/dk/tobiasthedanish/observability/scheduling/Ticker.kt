package dk.tobiasthedanish.observability.scheduling

import android.util.Log
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException

internal interface Ticker {
    fun start(intervalMillis: Long, block: () -> Unit)
    fun stop()
}

private const val TAG = "TickerImpl"

internal class TickerImpl(
    private val scheduler: Scheduler
): Ticker {
    private var future: Future<*>? = null

    override fun start(intervalMillis: Long, block: () -> Unit) {
        if (future != null) {
            Log.d(TAG, "Start called after ticker already has been started. Returning")
            return
        }
        try {
            future = scheduler.scheduleAtRate(
                runnable = block,
                intervalMillis,
                intervalMillis,
            )
        } catch (e: RejectedExecutionException) {
            Log.e(TAG, "Failed to start Ticker", e)
            return
        }
    }

    override fun stop() {
        future?.cancel(false)
        future = null
    }
}
