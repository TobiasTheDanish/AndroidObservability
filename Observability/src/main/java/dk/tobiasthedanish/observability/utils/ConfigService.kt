package dk.tobiasthedanish.observability.utils

import android.util.Log

internal interface ConfigService {
    val baseUrl: String
    val apiKey: String

    fun init(): Boolean
}

private const val TAG = "ConfigServiceImpl"

internal class ConfigServiceImpl(
    private val manifestReader: ManifestReader,
): ConfigService {
    override lateinit var baseUrl: String
    override lateinit var apiKey: String

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