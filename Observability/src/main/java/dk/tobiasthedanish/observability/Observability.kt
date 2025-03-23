package dk.tobiasthedanish.observability

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import dk.tobiasthedanish.observability.tracing.Trace
import java.util.concurrent.atomic.AtomicBoolean

object Observability {
    private val isInitialized = AtomicBoolean(false)

    private lateinit var observability: ObservabilityInternal

    @JvmStatic
    //@JvmOverloads
    fun init (context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            val application = context.applicationContext as Application

            observability = ObservabilityInternal(ObservabilityConfigInternalImpl(application))
            observability.init()
        }
    }

    @JvmStatic
    fun start() {
        if (isInitialized.get())
            observability.start()
    }

    @JvmStatic
    fun stop() {
        if (isInitialized.get())
            observability.stop()
    }

    @JvmStatic
    fun onNavigation(route: String) {
        Log.d("Observability", "Navigation to route: $route")
        if (isInitialized.get()) {
            observability.onNavigation(route)
        }
    }

    @JvmStatic
    fun createTrace(name: String): Trace? {
        if (isInitialized.get()) {
            return observability.createTrace(name)
        }
        return null
    }

    @JvmStatic
    fun startTrace(name: String): Trace? {
        if (isInitialized.get()) {
            return observability.startTrace(name)
        }
        return null
    }

    @VisibleForTesting
    internal fun initInstrumentationTest(configInternal: ObservabilityConfigInternal) {
        isInitialized.set(true)
        observability = ObservabilityInternal(configInternal)
        observability.init()
        this.start()
    }
}