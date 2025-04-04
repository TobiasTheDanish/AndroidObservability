package dk.tobiasthedanish.observability.export

import android.util.Log
import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.http.EventDTO
import dk.tobiasthedanish.observability.http.HttpResponse
import dk.tobiasthedanish.observability.http.InternalHttpClient
import dk.tobiasthedanish.observability.http.SessionDTO
import dk.tobiasthedanish.observability.http.TraceDTO
import dk.tobiasthedanish.observability.scheduling.Scheduler
import dk.tobiasthedanish.observability.scheduling.Ticker
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.storage.Database
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

internal interface Exporter: Collector {
    fun resume()
    fun pause()
    fun export(sessionId: String)
}

private const val DEFAULT_TIME_BETWEEN_EXPORTS = 30_000L
private const val TAG = "ExporterImpl"

internal class ExporterImpl(
    private val ticker: Ticker,
    private val httpService: InternalHttpClient,
    private val database: Database,
    private val sessionManager: SessionManager,
    private val scheduler: Scheduler,
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
            export()
            ticker.stop()
        }
    }

    private fun export() {
        val sessionId = sessionManager.getSessionId()

        export(sessionId)
    }

    override fun export(sessionId: String) {
        Log.d(TAG, "Exporting sessionId $sessionId")
        if (isExporting.compareAndSet(false, true)) {
            try {
                val data = database.getDataForExport(sessionId)
                if (data.sessionEntity == null && data.eventEntities.isEmpty() && data.traceEntities.isEmpty()) {
                    Log.i(TAG, "No data to export returning early")
                    return
                }

                Log.d(TAG, "Data to export: $data")

                val futures = mutableListOf<Future<*>>()
                if (data.sessionEntity != null) {
                    val future = try {
                        scheduler.start {
                            val response = httpService.exportSession(
                                SessionDTO(
                                    id = data.sessionEntity.id,
                                    installationId = "",
                                    createdAt = data.sessionEntity.createdAt,
                                    crashed = data.sessionEntity.crashed,
                                )
                            )

                            when(response) {
                                is HttpResponse.Success -> {
                                    database.setSessionExported(data.sessionEntity.id)
                                }
                                is HttpResponse.Error -> {
                                    Log.e(TAG, "Failed to export session with id: $sessionId")
                                }
                            }
                        }
                    } catch (e: RejectedExecutionException) {
                        Log.e(
                            TAG,
                            "Execution of session export was rejected, for session with id: $sessionId",
                            e
                        )
                        null
                    }
                    if (future != null) {
                        futures.add(future)
                    }
                }

                if (data.eventEntities.isNotEmpty()) {
                    val future = try {
                        scheduler.start {
                            data.eventEntities.forEach { entity ->
                                val response = httpService.exportEvent(EventDTO(
                                    id = entity.id,
                                    sessionId = entity.sessionId,
                                    type = entity.type,
                                    serializedData = entity.serializedData,
                                    createdAt = entity.createdAt,
                                ))

                                when(response) {
                                    is HttpResponse.Success -> {
                                        database.setEventExported(entity.id)
                                    }
                                    is HttpResponse.Error -> {
                                        Log.e(TAG, "Failed to export event with id: ${entity.id}")
                                    }
                                }
                            }
                        }
                    } catch (e: RejectedExecutionException) {
                        Log.e(
                            TAG,
                            "Execution of event export was rejected",
                            e
                        )
                        null
                    }
                    if (future != null) {
                        futures.add(future)
                    }
                }

                if (data.traceEntities.isNotEmpty()) {
                    val future = try {
                        scheduler.start {
                            data.traceEntities.forEach { entity ->
                                val response = httpService.exportTrace(
                                    TraceDTO(
                                        traceId = entity.traceId,
                                        groupId = entity.groupId,
                                        parentId = entity.parentId,
                                        sessionId = entity.sessionId,
                                        name = entity.name,
                                        errorMessage = entity.errorMessage,
                                        status = entity.status,
                                        startTime = entity.startTime,
                                        endTime = entity.endTime,
                                        hasEnded = entity.hasEnded,
                                    )
                                )

                                when (response) {
                                    is HttpResponse.Success -> {
                                        database.setTraceExported(entity.traceId)
                                    }

                                    is HttpResponse.Error -> {
                                        Log.e(
                                            TAG,
                                            "Failed to export trace with id: ${entity.traceId}"
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: RejectedExecutionException) {
                        Log.e(
                            TAG,
                            "Execution of trace export was rejected",
                            e
                        )
                        null
                    }
                    if (future != null) {
                        futures.add(future)
                    }
                }

                Log.d(TAG, "Futures: ${futures.size}")

                futures.forEachIndexed { index, it ->
                    try {
                        Log.d(TAG, "Waiting for future #$index: $it")
                        it.get()
                    } catch (e: CancellationException) {
                        Log.e(TAG, "Export future was cancelled", e)
                    }
                }
            } finally {
                Log.i(TAG, "Finished exporting sessionId: $sessionId")
                isExporting.set(false)
            }
        } else {
            Log.i(TAG, "Export already running")
        }
    }
}