package dk.tobiasthedanish.observability.tracing

import android.util.Log
import dk.tobiasthedanish.observability.collector.Collector
import java.util.concurrent.atomic.AtomicBoolean

internal interface TraceCollector: Collector {
    fun onStart(trace: InternalTrace)
    fun onEnded(trace: InternalTrace)
}

private const val TAG = "TraceCollectorImpl"

internal class TraceCollectorImpl(
    private val traceStore: TraceStore,
): TraceCollector {
    private val isRegistered = AtomicBoolean(false)

    override fun register() {
        isRegistered.set(true)
    }

    override fun unregister() {
        isRegistered.set(false)
    }

    override fun onStart(trace: InternalTrace) {
        if (isRegistered.get()) {
            Log.i(TAG, "Trace(${trace.name}) has been started")
        }
    }

    override fun onEnded(trace: InternalTrace) {
        if (isRegistered.get()) {
            Log.i(TAG, "Trace(${trace.name}) has ended")
            traceStore.store(trace)
        }
    }
}