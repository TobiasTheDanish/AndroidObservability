package dk.tobiasthedanish.observability.runtime

import dk.tobiasthedanish.observability.events.Event
import dk.tobiasthedanish.observability.scheduling.Ticker
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.EventEntity
import dk.tobiasthedanish.observability.storage.ExportEntity
import dk.tobiasthedanish.observability.storage.MemoryUsageEntity
import dk.tobiasthedanish.observability.storage.SessionEntity
import dk.tobiasthedanish.observability.storage.TraceEntity

internal class FakeTicker: Ticker {
    override fun start(intervalMillis: Long, block: () -> Unit) {
        block()
    }

    override fun stop() { }
}

internal class FakeMemoryInspector: MemoryInspector {
    companion object {
        val memoryUsage: MemoryUsage
            get() = MemoryUsage(
                freeMemory = 10L,
                usedMemory = 4L,
                totalMemory = 14L,
                maxMemory = 25L,
                availableHeapSpace = 21L,
            )
    }

    override fun freeMemory(): Long {
        return 10L
    }

    override fun usedMemory(): Long {
        return 4L
    }

    override fun totalMemory(): Long {
        return 14L
    }

    override fun maxMemory(): Long {
        return 25L
    }

    override fun availableHeapSpace(): Long {
        return 21L
    }
}

internal class FakeDatabase: Database {
    val dataMap = mutableMapOf<String, MutableList<Any>>()

    override fun createSession(session: SessionEntity) {

    }

    override fun getSession(sessionId: String): SessionEntity? {
        return null
    }

    override fun setSessionCrashed(sessionId: String) {
    }

    override fun setSessionExported(sessionId: String) {
    }

    override fun createEvent(event: EventEntity) {
    }

    override fun getEvent(eventId: String): EventEntity? {
        return null
    }

    override fun insertEvents(events: List<EventEntity>): Int {
        return 0
    }

    override fun setEventExported(eventId: String) {
    }

    override fun createTrace(trace: TraceEntity) {
    }

    override fun getTrace(traceId: String): TraceEntity? {
        return null
    }

    override fun insertTraces(traces: List<TraceEntity>): Int {
        return 0
    }

    override fun setTraceExported(traceId: String) {
    }

    override fun getMemoryUsage(id: String): MemoryUsageEntity? {
        return null
    }

    override fun createMemoryUsage(data: MemoryUsageEntity) {
    }

    override fun insertMemoryUsages(data: List<MemoryUsageEntity>): Int {
        val currentData = dataMap.getOrDefault(MemoryUsageEntity::class.java.name, mutableListOf())
        currentData.addAll(data)
        dataMap[MemoryUsageEntity::class.java.name] = currentData

        return 0
    }

    override fun setMemoryUsageExported(id: String) {

    }

    override fun getDataForExport(sessionId: String): ExportEntity {
        return ExportEntity(
            null,
            emptyList(),
            emptyList(),
            emptyList(),
        )
    }

    override fun deleteOldExportedSessions(currentSessionId: String) {
    }
}

internal class FakeSessionManager: SessionManager {
    override fun onAppForeground() {

    }

    override fun init() {
    }

    override fun getSessionId(): String {
        return "1234"
    }

    override fun <T : Any> onEventTracked(event: Event<T>) {
    }

    override fun onAppBackground() {
    }
}