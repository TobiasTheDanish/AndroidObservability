package dk.tobiasthedanish.observability.tracing

import android.util.Log
import dk.tobiasthedanish.observability.time.TimeProvider
import java.util.concurrent.atomic.AtomicBoolean

internal interface InternalTrace: Trace {
    val startTime: Long
    val endTime: Long
}

internal class TraceImpl(
    override var groupId: String,
    override val traceId: String,
    override var parentId: String?,
    override val name: String,
    private val traceCollector: TraceCollector,
    private val timeProvider: TimeProvider
) : InternalTrace {
    override var status: TraceStatus = TraceStatus.Ok
        private set
    override var startTime: Long = 0
    override var endTime: Long = 0
    private var hasEnded = AtomicBoolean(false)

    override fun setParent(parent: Trace): Trace {
        this.parentId = parent.traceId
        this.groupId = parent.groupId

        return this
    }

    override fun setStatus(status: TraceStatus): Trace {
        this.status = status
        return this
    }

    override fun start() {
        if (hasEnded.get() || startTime != 0L) {
            Log.w("Trace", "Attempted to start Trace($name) that is already ended")
        }
        startTime = timeProvider.now()
        traceCollector.onStart(this)
    }

    override fun end() {
        hasEnded.set(true)
        endTime = timeProvider.now()
        traceCollector.onEnded(this)
    }

    override fun hasEnded(): Boolean {
        return hasEnded.get()
    }
}