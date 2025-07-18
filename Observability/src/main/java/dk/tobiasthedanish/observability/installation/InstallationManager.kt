package dk.tobiasthedanish.observability.installation

import android.os.Build
import dk.tobiasthedanish.observability.http.HttpResponse
import dk.tobiasthedanish.observability.http.InstallationDTO
import dk.tobiasthedanish.observability.http.InstallationDataDTO
import dk.tobiasthedanish.observability.http.InternalHttpClient
import dk.tobiasthedanish.observability.scheduling.Scheduler
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.LocalPreferencesDataStore
import dk.tobiasthedanish.observability.utils.Logger
import java.util.concurrent.Future

internal interface InstallationManager {
    val installationId: String

    fun init()
}

private const val TAG = "InstallationManagerImpl"

internal class InstallationManagerImpl(
    private val preferencesDataStore: LocalPreferencesDataStore,
    private val idFactory: IdFactory,
    private val scheduler: Scheduler,
    private val httpService: InternalHttpClient,
    private val timeProvider: TimeProvider,
    private val logger: Logger = Logger(TAG),
): InstallationManager {
    private var _installationId: String? = null

    override val installationId: String
        get() = requireNotNull(_installationId) {
            "Installation manager must be initialized before accessing current installation id"
        }

    override fun init() {
        val future: Future<String?> = scheduler.start {
            preferencesDataStore.getInstallationId()
        }
        _installationId = future.get() ?: newInstallationId()
    }

    private fun newInstallationId(): String {
        val newId = idFactory.uuid()

        scheduler.start {
            preferencesDataStore.setInstallationId(newId)
            val response = httpService.exportInstallation(InstallationDTO(
                id = newId,
                data = InstallationDataDTO(
                    sdkVersion = Build.VERSION.SDK_INT,
                    model = Build.MODEL,
                    brand = Build.BRAND,
                ),
                createdAt = timeProvider.now()
            ))
            when (response) {
                is HttpResponse.Success -> logger.info("Installation exported successfully")
                is HttpResponse.Error -> logger.error("Failed to export installation id")
            }
        }

        return newId
    }
}