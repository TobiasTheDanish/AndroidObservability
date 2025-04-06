package dk.tobiasthedanish.observability.events

import android.util.Log
import dk.tobiasthedanish.observability.exception.ExceptionEvent
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEvent
import dk.tobiasthedanish.observability.navigation.NavigationEvent
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.EventEntity
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.isUnhandledException
import kotlinx.serialization.json.Json
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

internal interface EventStore {
    fun <T: Any>store(event: Event<T>)
    fun flush()
}

private const val TAG = "EventStoreImpl"

internal class EventStoreImpl(
    private val db: Database,
    private val idFactory: IdFactory,
): EventStore {
    private val queue by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LinkedBlockingQueue<EventEntity>(30)
    }
    private var isFlushing = AtomicBoolean(false)

    override fun <T: Any> store(event: Event<T>) {
        val serializedData: String = when (event.type) {
            EventTypes.UNHANDLED_EXCEPTION ->
                Json.encodeToString(ExceptionEvent.serializer(), event.data as ExceptionEvent)

            EventTypes.LIFECYCLE_APP ->
                Json.encodeToString(AppLifecycleEvent.serializer(), event.data as AppLifecycleEvent)

            EventTypes.LIFECYCLE_ACTIVITY ->
                Json.encodeToString(ActivityLifecycleEvent.serializer(), event.data as ActivityLifecycleEvent)

            EventTypes.NAVIGATION ->
                Json.encodeToString(NavigationEvent.serializer(), event.data as NavigationEvent)

            else -> ""
        }

        val entity = EventEntity(
            id = idFactory.uuid(),
            type = event.type,
            sessionId = event.sessionId,
            createdAt = event.timestamp,
            serializedData = serializedData,
        )

        if (event.isUnhandledException() || !queue.offer(entity)) {
            db.createEvent(entity)
            flush()
        }
    }

    override fun flush() {
        if (isFlushing.compareAndSet(false, true)) {
            try {
                val eventList = mutableListOf<EventEntity>()
                queue.drainTo(eventList)

                if (eventList.isEmpty()) {
                    return
                }

                if(!db.insertEvents(eventList)) {
                    Log.e(TAG, "Failed to insert ${eventList.size} events")
                } else {
                    Log.i(TAG, "Successfully inserted ${eventList.size} events")
                }
            } finally {
                isFlushing.set(false)
            }
        }
    }
}