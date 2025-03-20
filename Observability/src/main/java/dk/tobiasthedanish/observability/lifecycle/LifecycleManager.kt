package dk.tobiasthedanish.observability.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean

internal class LifecycleManager(
    private val application: Application
): Application.ActivityLifecycleCallbacks {
    private var isRegistered = AtomicBoolean(false)
    private val startedActivities = mutableSetOf<String>()
    private val appLifecycleListeners = mutableListOf<AppLifecycleListener>()
    private val activityLifecycleListeners = mutableListOf<ActivityLifecycleCollector>()

    fun register() {
        if (isRegistered.compareAndSet(false, true)) {
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    fun unregister() {
        if (isRegistered.compareAndSet(true, false)) {
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    fun addListener(listener: AppLifecycleListener) {
        appLifecycleListeners.add(listener)
    }

    fun removeListener(listener: AppLifecycleListener) {
        appLifecycleListeners.remove(listener)
    }

    fun addListener(listener: ActivityLifecycleCollector) {
        activityLifecycleListeners.add(listener)
    }

    fun removeListener(listener: ActivityLifecycleCollector) {
        activityLifecycleListeners.remove(listener)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityLifecycleListeners.forEach { it.onActivityCreated(activity, savedInstanceState) }
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities.isEmpty()) {
            // App foreground
            appLifecycleListeners.forEach { it.onAppForeground() }
        }
        val hash = Integer.toHexString(System.identityHashCode(activity))
        startedActivities.add(hash)
    }

    override fun onActivityResumed(activity: Activity) {
        activityLifecycleListeners.forEach { it.onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
        activityLifecycleListeners.forEach { it.onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
        val hash = Integer.toHexString(System.identityHashCode(activity))
        startedActivities.remove(hash)
        if (startedActivities.isEmpty()) {
            appLifecycleListeners.forEach { it.onAppBackground() }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }

    override fun onActivityDestroyed(activity: Activity) {
        activityLifecycleListeners.forEach { it.onActivityDestroyed(activity) }
    }
}