package dk.tobiasthedanish.observability.events

internal interface EventStore {
    fun <T: Any>store(event: Event<T>)
    fun clear()
}

internal class EventStoreImpl(): EventStore {
    override fun <T : Any> store(event: Event<T>) {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

}