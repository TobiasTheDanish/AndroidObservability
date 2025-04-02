package dk.tobiasthedanish.observability.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

data class ManifestMetadata(val baseUrl: String?, val apiKey: String?)

internal interface ManifestReader {
    fun read(): ManifestMetadata?
}

private const val TAG = "ManifestReaderImpl"

internal class ManifestReaderImpl(private val context: Context): ManifestReader {
    private val urlKey = "dk.tobiasthedanish.observability.API_URL"
    private val apiKey = "dk.tobiasthedanish.observability.API_KEY"

    override fun read(): ManifestMetadata? {
        val bundle = try {
            getManifestBundle()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Could not load Android Manifest from package ${context.packageName}")
            return null
        }

        return ManifestMetadata(
            baseUrl = bundle.getString(urlKey),
            apiKey = bundle.getString(apiKey),
        )
    }

    private fun getManifestBundle(): Bundle {
        return context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        ).metaData
    }
}