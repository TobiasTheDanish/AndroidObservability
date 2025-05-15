package dk.tobiasthedanish.observability.events

import dk.tobiasthedanish.observability.exception.ExceptionEvent
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEvent
import dk.tobiasthedanish.observability.navigation.NavigationEvent
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.EventEntity
import dk.tobiasthedanish.observability.utils.ConfigService
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.Logger
import dk.tobiasthedanish.observability.utils.isUnhandledException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KType

internal interface EventStore {
    fun <T: Any>store(event: Event<T>)
    fun <T: Any>storeCustom(event: Event<T>, kType: KType)
    fun flush()
}

private const val TAG = "EventStoreImpl"

internal class EventStoreImpl(
    private val db: Database,
    private val idFactory: IdFactory,
    private val configService: ConfigService,
    private val logger: Logger = Logger(TAG),
): EventStore {
    private val queue by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LinkedBlockingQueue<EventEntity>(configService.maxEventsStoredBeforeFlush)
    }
    private var isFlushing = AtomicBoolean(false)

    override fun <T: Any> store(event: Event<T>) {
        val serializedData: String = when (event.type) {
            EventTypes.EXCEPTION ->
                Json.encodeToString(ExceptionEvent.serializer(), event.data as ExceptionEvent)

            EventTypes.LIFECYCLE_APP ->
                Json.encodeToString(AppLifecycleEvent.serializer(), event.data as AppLifecycleEvent)

            EventTypes.LIFECYCLE_ACTIVITY ->
                Json.encodeToString(ActivityLifecycleEvent.serializer(), event.data as ActivityLifecycleEvent)

            EventTypes.NAVIGATION ->
                Json.encodeToString(NavigationEvent.serializer(), event.data as NavigationEvent)

            else -> ""
        }

        storeSerialized(event, serializedData)
    }

    /**
     * @throws [EventStoreSerializationException] if the given data cannot be serialized to JSON, by kotlinx.serialization.json.JSON.
     */
    override fun <T : Any> storeCustom(event: Event<T>, kType: KType) {
        val serializedData = try {
            Json.encodeToString(Json.serializersModule.serializer(kType), event.data)
        } catch (e: SerializationException) {
            throw EventStoreSerializationException(e)
        }

        storeSerialized(event, serializedData)
    }

    private fun <T: Any>storeSerialized(event: Event<T>, serializedData: String) {
        val entity = EventEntity(
            id = idFactory.uuid(),
            type = event.type,
            sessionId = event.sessionId,
            createdAt = event.timestamp,
            serializedData = serializedData,
        )
        logger.debug("Storing new event: ${entity.id} ${entity.type}")

        if (event.isUnhandledException() || !queue.offer(entity)) {
            db.createEvent(entity)
            flush()
        }

    }

    override fun flush() {
        logger.debug( "Flush triggered")
        if (isFlushing.compareAndSet(false, true)) {
            try {
                val eventList = mutableListOf<EventEntity>()
                queue.drainTo(eventList)

                if (eventList.isEmpty()) {
                    return
                }

                val failed = db.insertEvents(eventList)
                if(failed > 0) {
                    logger.error("Failed to insert $failed events")
                    if (failed < eventList.size)
                        logger.info("Successfully inserted ${eventList.size-failed} events")
                } else {
                    logger.info("Successfully inserted ${eventList.size} events")
                }
            } finally {
                isFlushing.set(false)
            }
        }
    }
}