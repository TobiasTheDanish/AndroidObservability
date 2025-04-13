package dk.tobiasthedanish.observability.export

import android.util.Log
import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.http.EventDTO
import dk.tobiasthedanish.observability.http.ExportDTO
import dk.tobiasthedanish.observability.http.HttpResponse
import dk.tobiasthedanish.observability.http.InternalHttpClient
import dk.tobiasthedanish.observability.http.SessionDTO
import dk.tobiasthedanish.observability.http.TraceDTO
import dk.tobiasthedanish.observability.scheduling.Scheduler
import dk.tobiasthedanish.observability.scheduling.Ticker
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.storage.Database
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

internal interface Exporter : Collector {
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
) : Exporter {
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

    private fun exportSession(sessionDTO: SessionDTO) {
        val future = try {
            scheduler.start {
                val response = httpService.exportSession(sessionDTO)

                when (response) {
                    is HttpResponse.Success -> {
                        database.setSessionExported(sessionDTO.id)
                    }

                    is HttpResponse.Error -> {
                        Log.e(TAG, "Failed to export session with id: ${sessionDTO.id}")
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            Log.e(
                TAG,
                "Execution of session export was rejected, for session with id: ${sessionDTO.id}",
                e
            )
            null
        }

        future?.get()
    }

    private fun exportCollection(collection: ExportDTO) {
        val future = try {
            scheduler.start {
                val response = httpService.exportCollection(collection)

                when (response) {
                    is HttpResponse.Success -> {
                        if (collection.session != null) {
                            database.setSessionExported(collection.session.id)
                        }

                        collection.events.forEach {
                            database.setEventExported(it.id)
                        }
                        collection.traces.forEach {
                            database.setTraceExported(it.traceId)
                        }
                    }

                    is HttpResponse.Error -> {
                        Log.e(TAG, "Failed to export collection for session with id: ${collection.session?.id}")
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            Log.e(
                TAG,
                "Execution of collection export was rejected, for session with id: ${collection.session?.id}",
                e
            )
            null
        }

        future?.get()
    }

    override fun export(sessionId: String) {
        Log.d(TAG, "Exporting sessionId $sessionId")
        if (isExporting.compareAndSet(false, true)) {
            scheduler.start {
                try {
                    val data = database.getDataForExport(sessionId)
                    if (data.sessionEntity == null && data.eventEntities.isEmpty() && data.traceEntities.isEmpty()) {
                        Log.i(TAG, "No data to export returning early")
                        return@start
                    }

                    Log.d(TAG, "Data to export: $data")

                    if (data.sessionEntity != null && data.eventEntities.isEmpty() && data.traceEntities.isEmpty()) {
                        exportSession(
                            SessionDTO(
                                id = data.sessionEntity.id,
                                installationId = "",
                                createdAt = data.sessionEntity.createdAt,
                                crashed = data.sessionEntity.crashed,
                            )
                        )
                    } else {
                        exportCollection(
                            ExportDTO(
                                session = if (data.sessionEntity != null) SessionDTO(
                                    id = data.sessionEntity.id,
                                    installationId = "",
                                    createdAt = data.sessionEntity.createdAt,
                                    crashed = data.sessionEntity.crashed,
                                ) else null,
                                events = data.eventEntities.map {
                                    EventDTO(
                                        id = it.id,
                                        sessionId = it.sessionId,
                                        serializedData = it.serializedData,
                                        type = it.type,
                                        createdAt = it.createdAt
                                    )
                                },
                                traces = data.traceEntities.map {
                                    TraceDTO(
                                        traceId = it.traceId,
                                        groupId = it.groupId,
                                        sessionId = it.sessionId,
                                        parentId = it.parentId,
                                        name = it.name,
                                        status = it.status,
                                        errorMessage = it.errorMessage,
                                        startTime = it.startTime,
                                        endTime = it.endTime,
                                        hasEnded = it.hasEnded,
                                    )
                                }
                            )
                        )
                    }
                } finally {
                    Log.i(TAG, "Finished exporting sessionId: $sessionId")
                    isExporting.set(false)
                }
            }
        } else {
            Log.i(TAG, "Export already running")
        }
    }
}