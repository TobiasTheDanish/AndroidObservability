package dk.tobiasthedanish.observability.export

import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.scheduling.Ticker
import java.util.concurrent.atomic.AtomicBoolean

internal interface Exporter: Collector {
    fun resume()
    fun pause()
}

// Ticker class which this can subscribe to (ExecutorService)
// A way to create http requests

private const val DEFAULT_TIME_BETWEEN_EXPORTS = 30_000L

internal class ExporterImpl(
    private val ticker: Ticker
): Exporter {
    private var isRegistered = AtomicBoolean(false)
    private var isExporting = AtomicBoolean(false)

    override fun resume() {
        if (isRegistered.get()) {
            ticker.start(DEFAULT_TIME_BETWEEN_EXPORTS) {
                export()
            }
        }
    }

    override fun pause() {
        if (isRegistered.get()) {
            ticker.stop()
        }
    }

    override fun register() {
        if (isRegistered.compareAndSet(false, true)) {
            resume()
        }
    }

    override fun unregister() {
        if (isRegistered.compareAndSet(true, false)) {
            pause()
        }
    }

    private fun export() {

    }
}