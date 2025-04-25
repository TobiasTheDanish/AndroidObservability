package dk.tobiasthedanish.observability.tracing

import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.IdFactory

internal interface TraceFactory {
    fun createTrace(name: String): Trace
    fun startTrace(name: String): Trace
}

internal class TraceFactoryImpl(
    private val timeProvider: TimeProvider,
    private val traceCollector: TraceCollector,
    private val idFactory: IdFactory,
): TraceFactory {
    override fun createTrace(name: String): TraceImpl {
        return TraceImpl(
            name = name,
            groupId = idFactory.uuid(),
            traceId = idFactory.uuid(),
            parentId = null,
            traceCollector = traceCollector,
            timeProvider = timeProvider
        )
    }

    override fun startTrace(name: String): TraceImpl {
        val trace = createTrace(name)
        trace.start()
        return trace
    }
}