package dk.tobiasthedanish.observability.lifecycle

import android.app.Activity
import android.os.Bundle

internal interface AppLifecycleListener {
    fun onAppForeground()
    fun onAppBackground()
}

internal interface ActivityLifecycleListener {
    fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?)
    fun onActivityResumed(activity: Activity)
    fun onActivityPaused(activity: Activity)
    fun onActivityDestroyed(activity: Activity)
}
