package dk.tobiasthedanish.observability.installation

import dk.tobiasthedanish.observability.http.HttpResponse
import dk.tobiasthedanish.observability.http.InstallationDTO
import dk.tobiasthedanish.observability.http.InternalHttpClient
import dk.tobiasthedanish.observability.scheduling.Scheduler
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.LocalPreferencesDataStore
import dk.tobiasthedanish.observability.utils.Logger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.util.concurrent.Future

class InstallationManagerImplTest {
    private val mockPreferencesDataStore: LocalPreferencesDataStore = mock()
    private val mockIdFactory: IdFactory = mock()
    private val mockHttpService: InternalHttpClient = mock()
    private val mockTimeProvider: TimeProvider = mock()
    private val mockScheduler: Scheduler = mock {
        doReturn(mock<Future<Unit>>()).on { start(block = any<suspend () -> Unit>()) }
    }
    private val mockLogger: Logger = mock<Logger>()

    @Test
    fun testStoredInstallationId() {
        // GIVEN DataStore Has Installation Id
        val installationManager = InstallationManagerImpl(
            preferencesDataStore = mockPreferencesDataStore,
            idFactory = mockIdFactory,
            scheduler = mockScheduler.stub {
                doReturn(mock<Future<String?>> {
                    on { get() }.thenReturn("1234")
                }).on { start(block = any<suspend () -> String?>()) }
            },
            httpService = mockHttpService.stub {
                onBlocking { exportInstallation(any<InstallationDTO>()) }.thenReturn(HttpResponse.Success("Success"))
            },
            timeProvider = mockTimeProvider,
            logger = mockLogger,
        )

        // WHEN Init Is Called
        installationManager.init()

        // THEN Installation Id Equals Id From DataStore
        assertEquals("1234", installationManager.installationId)
    }

    @Test
    fun testNoStoredInstallationId() = runTest {
        // GIVEN No id is stored in data store
        val installationManager = InstallationManagerImpl(
            preferencesDataStore = mockPreferencesDataStore.stub {
                onBlocking { setInstallationId(any<String>()) }.thenReturn(Unit)
            },
            idFactory = mockIdFactory.stub {
                on { uuid() }.thenReturn("12345")
            },
            scheduler = mockScheduler.stub {
                doReturn(mock<Future<String?>> {
                    on { get() }.thenReturn(null)
                }).on { start(block = any<suspend () -> String?>()) }
            },
            httpService = mockHttpService,
            timeProvider = mockTimeProvider,
            logger = mockLogger,
        )

        // WHEN init is called
        installationManager.init()

        testScheduler.runCurrent()

        // THEN the installation id equals the id from the id factory
        assertEquals("12345", installationManager.installationId)
    }

    @Test
    fun testNoInitCalled() {
        // GIVEN init is never called on installation manager
        val installationManager = InstallationManagerImpl(
            preferencesDataStore = mockPreferencesDataStore,
            idFactory = mockIdFactory,
            scheduler = mockScheduler,
            httpService = mockHttpService,
            timeProvider = mockTimeProvider,
            logger = mockLogger,
        )

        assertThrows(IllegalArgumentException::class.java) {
            // WHEN installationId is attempted to be fetched
            // THEN an IllegalArgumentException is thrown
            installationManager.installationId
        }
    }
}