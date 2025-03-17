package dk.tobiasthedanish.observability.events

import dk.tobiasthedanish.observability.exception.ExceptionEvent
import dk.tobiasthedanish.observability.lifecycle.ActivityLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEvent
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.EventEntity
import dk.tobiasthedanish.observability.utils.IdFactory
import kotlinx.serialization.json.Json

internal interface EventStore {
    fun <T: Any>store(event: Event<T>)
    fun clear()
}

internal class EventStoreImpl(
    private val db: Database,
    private val idFactory: IdFactory,
): EventStore {
    override fun <T : Any> store(event: Event<T>) {
        val serializedData: String = when (event.type) {
            EventTypes.UNHANDLED_EXCEPTION ->
                Json.encodeToString(ExceptionEvent.serializer(), event.data as ExceptionEvent)

            EventTypes.LIFECYCLE_APP ->
                Json.encodeToString(AppLifecycleEvent.serializer(), event.data as AppLifecycleEvent)

            EventTypes.LIFECYCLE_ACTIVITY ->
                Json.encodeToString(ActivityLifecycleEvent.serializer(), event.data as ActivityLifecycleEvent)

            else -> ""
        }

        val entity = EventEntity(
            id = idFactory.uuid(),
            type = event.type,
            sessionId = event.sessionId,
            createdAt = event.timestamp,
            serializedData = serializedData,
        )

        db.createEvent(entity)
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

}