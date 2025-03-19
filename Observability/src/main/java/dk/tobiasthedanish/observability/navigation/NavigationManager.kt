package dk.tobiasthedanish.observability.navigation

import android.util.Log
import dk.tobiasthedanish.observability.collector.Collector

internal interface NavigationManager: Collector {
    fun addCollector(collector: NavigationCollector)
    fun removeCollector(collector: NavigationCollector)

    fun onNavigation(route: String)
}

private const val TAG = "NavigationManagerImpl"

internal class NavigationManagerImpl(): NavigationManager {
    private val collectors = mutableSetOf<NavigationCollector>()

    override fun register() {
        collectors.forEach { it.register() }
    }

    override fun unregister() {
        collectors.forEach { it.unregister() }
    }

    override fun addCollector(collector: NavigationCollector) {
        this.collectors.add(collector)
    }

    override fun removeCollector(collector: NavigationCollector) {
        this.collectors.remove(collector)
    }

    override fun onNavigation(route: String) {
        Log.d(TAG, "Navigation to route: $route")
        collectors.forEach { it.onNavigation(route) }
    }
}