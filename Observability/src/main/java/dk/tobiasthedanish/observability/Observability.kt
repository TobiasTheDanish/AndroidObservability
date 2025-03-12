package dk.tobiasthedanish.observability

import android.app.Application
import android.content.Context
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
        observability.start()
    }

    @JvmStatic
    fun stop() {
        observability.stop()
    }
}