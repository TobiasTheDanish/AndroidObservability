package dk.tobiasthedanish.observability.utils

import android.util.Log
import dk.tobiasthedanish.observability.ObservabilityConfig
import kotlin.time.Duration

internal interface ConfigService {
    val baseUrl: String
    val apiKey: String
    val autoStart: Boolean
    val timeBetweenExports: Duration
    val maxSessionDuration: Duration
    val maxSessionTimeBetweenEvents: Duration
    val maxEventsStoredBeforeFlush: Int
    val maxTracesStoredBeforeFlush: Int

    fun init(): Boolean
}

private const val TAG = "ConfigServiceImpl"

internal class ConfigServiceImpl(
    private val manifestReader: ManifestReader,
    private val userConfig: ObservabilityConfig,
): ConfigService {
    // ANDROID MANIFEST KEYS
    override lateinit var baseUrl: String
    override lateinit var apiKey: String

    // USER CONFIG KEYS
    override val autoStart: Boolean by lazy { userConfig.autoStart }
    override val timeBetweenExports: Duration by lazy { userConfig.timeBetweenExports }
    override val maxSessionDuration: Duration by lazy { userConfig.maxSessionDuration }
    override val maxSessionTimeBetweenEvents: Duration by lazy { userConfig.maxSessionTimeBetweenEvents }
    override val maxEventsStoredBeforeFlush: Int by lazy { userConfig.maxEventsStoredBeforeFlush }
    override val maxTracesStoredBeforeFlush: Int by lazy { userConfig.maxTracesStoredBeforeFlush }

    override fun init(): Boolean {
        manifestReader.read()?.let {
            if (it.apiKey == null || it.baseUrl == null) {
                if (it.apiKey == null) {
                    Log.d(TAG, "Api Key is missing from AndroidManifest.xml")
                }
                if (it.baseUrl == null) {
                    Log.d(TAG, "Server url is missing from AndroidManifest.xml")
                }
                return false
            }

            baseUrl = it.baseUrl
            apiKey = it.apiKey
        }

        return true
    }
}