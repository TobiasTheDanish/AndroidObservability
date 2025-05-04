package dk.tobiasthedanish.observability.events

import dk.tobiasthedanish.observability.utils.IdFactory
import kotlinx.serialization.Serializable
import org.junit.Assert.*

import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.reflect.typeOf

@Serializable
data class SerializableClass(val name: String)
data class UnSerializableClass(val name: String)

class EventStoreImplTest {
    private val mockIdFactory = mock<IdFactory> {
        on { uuid() }.thenReturn("Very unique id")
    }

    private val store: EventStore = EventStoreImpl(
        db = mock(),
        idFactory = mockIdFactory,
        logger = mock(),
    )

    @Test
    fun storeCustomSerializable() {
        val data = SerializableClass("This should work")
        val event = Event(
            data = data,
            type = EventTypes.CUSTOM,
            timestamp = 12345L,
            sessionId = "Session ID"
        )

        try {
            store.storeCustom(event, typeOf<SerializableClass>())
        } catch (e: Exception) {
            throw AssertionError("storeCustom threw exception, when it shouldn't", e)
        }
    }

    @Test
    fun storeCustomUnSerializable() {
        val data = UnSerializableClass("This should NOT work")
        val event = Event(
            data = data,
            type = EventTypes.CUSTOM,
            timestamp = 12345L,
            sessionId = "Session ID"
        )

        assertThrows(EventStoreSerializationException::class.java) {
            store.storeCustom(event, typeOf<UnSerializableClass>())
        }
    }
}