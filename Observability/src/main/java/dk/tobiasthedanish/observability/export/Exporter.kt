package dk.tobiasthedanish.observability.export

import dk.tobiasthedanish.observability.collector.Collector
import dk.tobiasthedanish.observability.http.EventDTO
import dk.tobiasthedanish.observability.http.ExportDTO
import dk.tobiasthedanish.observability.http.HttpResponse
import dk.tobiasthedanish.observability.http.InternalHttpClient
import dk.tobiasthedanish.observability.http.MemoryUsageDTO
import dk.tobiasthedanish.observability.http.SessionDTO
import dk.tobiasthedanish.observability.http.TraceDTO
import dk.tobiasthedanish.observability.installation.InstallationManager
import dk.tobiasthedanish.observability.scheduling.Scheduler
import dk.tobiasthedanish.observability.scheduling.Ticker
import dk.tobiasthedanish.observability.session.SessionManager
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.utils.ConfigService
import dk.tobiasthedanish.observability.utils.Logger
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

internal interface Exporter : Collector {
    fun resume()
    fun pause()
    fun export(sessionId: String)
    fun exportSessionCrash(sessionId: String)
}

private const val TAG = "ExporterImpl"

internal class ExporterImpl(
    private val ticker: Ticker,
    private val httpService: InternalHttpClient,
    private val database: Database,
    private val sessionManager: SessionManager,
    private val installationManager: InstallationManager,
    private val scheduler: Scheduler,
    private val configService: ConfigService,
    private val log: Logger = Logger(TAG)
) : Exporter {
    private var isRegistered = AtomicBoolean(false)
    private var isExporting = AtomicBoolean(false)

    override fun resume() {
        if (isRegistered.get()) {
            ticker.start(configService.timeBetweenExports.inWholeMilliseconds) {
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

    private fun exportSession(sessionDTO: SessionDTO): Future<Unit>? {
        return try {
            scheduler.start {
                val response = httpService.exportSession(sessionDTO)

                when (response) {
                    is HttpResponse.Success -> {
                        database.setSessionExported(sessionDTO.id)
                    }

                    is HttpResponse.Error -> {
                        log.error("Failed to export session with id: ${sessionDTO.id}")
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            log.error(
                "Execution of session export was rejected, for session with id: ${sessionDTO.id}",
                e
            )
            null
        }
    }

    private fun exportCollection(collection: ExportDTO): Future<Unit>? {
        return try {
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
                        log.error( "Failed to export collection for session with id: ${collection.session?.id}")
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            log.error(
                "Execution of collection export was rejected, for session with id: ${collection.session?.id}",
                e
            )
            null
        }
    }

    override fun exportSessionCrash(sessionId: String) {
        try {
            scheduler.start {
                val response = httpService.markSessionCrashed(sessionId)

                when (response) {
                    is HttpResponse.Success -> {
                        database.setSessionExported(sessionId)
                    }

                    is HttpResponse.Error -> {
                        log.error("Failed to export crash for session with id: $sessionId")
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            log.error(
                "Execution of session crash export was rejected, for session with id: $sessionId",
                e
            )
        }
    }

    override fun export(sessionId: String) {
        log.debug("Exporting sessionId $sessionId")
        if (isExporting.compareAndSet(false, true)) {
            scheduler.start {
                try {
                    val data = database.getDataForExport(sessionId)
                    if (data.sessionEntity == null && data.eventEntities.isEmpty() && data.traceEntities.isEmpty()) {
                        log.info("No data to export returning early")
                        return@start
                    }

                    log.debug("Data to export: $data")

                    val collectionFuture = if (data.sessionEntity != null && data.eventEntities.isEmpty() && data.traceEntities.isEmpty()) {
                        exportSession(
                            SessionDTO(
                                id = data.sessionEntity.id,
                                installationId = installationManager.installationId,
                                createdAt = data.sessionEntity.createdAt,
                                crashed = data.sessionEntity.crashed,
                            )
                        )
                    } else {
                        exportCollection(
                            ExportDTO(
                                session = if (data.sessionEntity != null) SessionDTO(
                                    id = data.sessionEntity.id,
                                    installationId = installationManager.installationId,
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

                    // EXPORT RESOURCE USAGE
                    val memoryFuture = try {
                        if (data.memoryUsageEntities.isEmpty()) {
                            null
                        } else {
                            scheduler.start {
                                val response = httpService.exportMemoryUsage(data.memoryUsageEntities.map { entity ->
                                    MemoryUsageDTO(
                                        id = entity.id,
                                        sessionId = entity.sessionId,
                                        installationId = installationManager.installationId,
                                        usedMemory = entity.usedMemory,
                                        freeMemory = entity.freeMemory,
                                        totalMemory = entity.totalMemory,
                                        maxMemory = entity.maxMemory,
                                        availableHeapSpace = entity.availableHeapSpace,
                                        createdAt = entity.createdAt,
                                    )
                                })

                                when (response) {
                                    is HttpResponse.Success -> {
                                        data.memoryUsageEntities.forEach {
                                            database.setMemoryUsageExported(it.id)
                                        }
                                    }

                                    is HttpResponse.Error -> {
                                        log.error( "Failed to export memory usages for session with id: $sessionId")
                                    }
                                }
                            }
                        }
                    } catch (e: RejectedExecutionException) {
                        log.error(
                            "Execution of memory usage export was rejected, for session with id: $sessionId",
                            e
                        )
                        null
                    }

                    collectionFuture?.get()
                    memoryFuture?.get()
                } finally {
                    log.info("Finished exporting sessionId: $sessionId")
                    isExporting.set(false)
                }
            }
        } else {
            log.info("Export already running")
        }
    }
}