package dk.tobiasthedanish.observability.tracing

import android.util.Log
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.TraceEntity
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

internal interface TraceStore {
    fun store(trace: InternalTrace)
    fun flush()
}

private const val TAG = "TraceStoreImpl"

internal class TraceStoreImpl(
    private val sessionManager: SessionManager,
    private val db: Database
): TraceStore {
    private val queue by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LinkedBlockingQueue<TraceEntity>(30)
    }
    private var isFlushing = AtomicBoolean(false)

    override fun store(trace: InternalTrace) {
        val errorMessage = when(trace.status) {
            is TraceStatus.Ok -> null
            is TraceStatus.Error -> (trace.status as TraceStatus.Error).message
        }
        val sessionId = sessionManager.getSessionId()

        val entity = TraceEntity(
            traceId = trace.traceId,
            groupId = trace.groupId,
            parentId = trace.parentId,
            sessionId = sessionId,
            name = trace.name,
            status = trace.status.name,
            errorMessage = errorMessage,
            startTime = trace.startTime,
            endTime = trace.endTime,
            hasEnded = trace.hasEnded(),
        )

        if (!queue.offer(entity)) {
            db.createTrace(entity)
            flush()
        }
    }

    override fun flush() {
        if (isFlushing.compareAndSet(false, true)) {
            try {
                val traceList = mutableListOf<TraceEntity>()
                queue.drainTo(traceList)

                if (traceList.isEmpty()) {
                    return
                }

                if(!db.insertTraces(traceList)) {
                    Log.e(TAG, "Failed to insert ${traceList.size} traces")
                } else {
                    Log.i(TAG, "Successfully inserted ${traceList.size} traces")
                }
            } finally {
                isFlushing.set(false)
            }
        }
    }
}