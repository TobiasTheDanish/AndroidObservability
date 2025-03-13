package dk.tobiasthedanish.observability.collector

internal interface Collector {
    fun register()
    fun unregister()
}