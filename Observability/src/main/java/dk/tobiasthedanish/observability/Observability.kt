package dk.tobiasthedanish.observability

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import dk.tobiasthedanish.observability.tracing.Trace
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import org.jetbrains.annotations.TestOnly

object Observability {
    @PublishedApi internal val isInitialized = AtomicBoolean(false)

    private lateinit var observability: ObservabilityInternal

    @JvmStatic
    @JvmOverloads
    fun init(context: Context, config: ObservabilityConfig = ObservabilityConfig()) {
        if (isInitialized.compareAndSet(false, true)) {
            val application = context.applicationContext as Application

            observability =
                    ObservabilityInternal(ObservabilityConfigInternalImpl(application, config))
            observability.init()
        }
    }

    @JvmStatic
    fun start() {
        if (isInitialized.get()) observability.start()
    }

    @JvmStatic
    fun stop() {
        if (isInitialized.get()) observability.stop()
    }

    @JvmStatic
    fun getInstallationId(): String {
        require(isInitialized.get()) {
            "Observability must be initialized before getting installation id"
        }
        return observability.getInstallationId()
    }

    @JvmStatic
    fun onNavigation(route: String) {
        if (isInitialized.get()) {
            observability.onNavigation(route)
        }
    }

    @JvmStatic
    fun exceptionHandled(throwable: Throwable, thread: Thread = Thread.currentThread()) {
        if (isInitialized.get()) {
            observability.exceptionHandled(thread, throwable)
        }
    }

    @JvmStatic
    fun <T : Any> trackEvent(data: T, kType: KType) {
        if (isInitialized.get()) {
            observability.trackEvent(data, kType)
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

    /**
     * Creates and starts a trace with the given [name]. If the provided [parent] is not null, then
     * that trace is set as the parent of the trace started in this function. The trace is then
     * passed to the function provided by [block]. When [block] returns the trace is ended, and the
     * result of block is returned.
     */
    @JvmStatic
    @JvmOverloads
    fun <T> traceFun(name: String, parent: Trace? = null, block: (Trace?) -> T): T {
        val trace = startTrace(name)
        if (parent != null) trace?.setParent(parent)
        val res = block(trace)
        trace?.end()

        return res
    }

    /**
     * Creates and starts a trace with the given [name]. If the provided [parent] is not null, then
     * that trace is set as the parent of the trace started in this function. The trace is then
     * passed to the function provided by [block]. When [block] returns the trace is ended, and the
     * result of block is returned.
     *
     * @throw IllegalArgumentException if init has not been called
     */
    @JvmStatic
    @JvmOverloads
    inline fun <reified T> traceFun(name: String, parent: Trace? = null, block: (Trace) -> T): T {
        require(isInitialized.get()) { "Cannot trace if Observability is not initialized!" }

        val trace =
                requireNotNull(createTrace(name)) {
                    "Cannot trace if Observability is not initialized!"
                }
        if (parent != null) trace.setParent(parent)
        val res = block(trace)
        trace.end()

        return res
    }

    @VisibleForTesting
    internal fun initInstrumentationTest(configInternal: ObservabilityConfigInternal) {
        this.stop()
        isInitialized.set(true)
        observability = ObservabilityInternal(configInternal)
        observability.init()
        this.start()
    }

    @TestOnly
    internal fun getSessionId(): String {
        return observability.getSessionId()
    }

    @TestOnly
    internal fun triggerExport() {
        observability.triggerExport()
    }
}

inline fun <reified T : Any> Observability.trackEvent(data: T) {
    trackEvent(data, typeOf<T>())
}

