package dk.tobiasthedanish.observability.http

import dk.tobiasthedanish.observability.utils.ConfigService
import dk.tobiasthedanish.observability.utils.Logger
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class InternalHttpClientImplTest {
    private val mockEnv: ConfigService = mock {
        on { apiKey }.thenReturn("secret")
        on { baseUrl }.thenReturn("http://localhost:8080")
    }
    private val mockLogger = mock<Logger>()

    private val internalHttpClient: InternalHttpClient = InternalHttpClientImpl(
        client = HttpClientFactory.client,
        env = mockEnv,
        logger = mockLogger
    )
    private val mockWebServer = MockWebServer()

    @Before
    fun setUp() {
        mockWebServer.start(8080)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testExportCollection() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"msg\":\"Success\"}"))

        val data = ExportDTO(
            session = SessionDTO(
                id = "",
                installationId = "",
                createdAt = 123456789,
            ),
            events = listOf(EventDTO(
                id = "",
                serializedData = "",
                type = "",
                createdAt = 123456789,
                sessionId = ""
            )),
            traces = listOf(TraceDTO(
                groupId = "",
                traceId = "",
                parentId = "",
                sessionId = "",
                name = "",
                status = "",
                errorMessage = "",
                startTime = 123456000,
                endTime = 123456789,
                hasEnded = true
            )),
        )

        val res = internalHttpClient.exportCollection(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Success)

        val body = (res as HttpResponse.Success).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Success\"}")
    }

    @Test
    fun testExportCollectionServerError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("{\"msg\":\"Internal Server Error\"}"))

        val data = ExportDTO(
            session = SessionDTO(
                id = "",
                installationId = "",
                createdAt = 123456789,
            ),
            events = listOf(EventDTO(
                id = "",
                serializedData = "",
                type = "",
                createdAt = 123456789,
                sessionId = ""
            )),
            traces = listOf(TraceDTO(
                groupId = "",
                traceId = "",
                parentId = "",
                sessionId = "",
                name = "",
                status = "",
                errorMessage = "",
                startTime = 123456000,
                endTime = 123456789,
                hasEnded = true
            )),
        )

        val res = internalHttpClient.exportCollection(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Error.ServerError)

        val body = (res as HttpResponse.Error.ServerError).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Internal Server Error\"}")
    }

    @Test
    fun exportSession() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"msg\":\"Success\"}"))

        val data = SessionDTO(
            id = "",
            installationId = "",
            createdAt = 123456789,
        )

        val res = internalHttpClient.exportSession(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Success)

        val body = (res as HttpResponse.Success).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Success\"}")
    }

    @Test
    fun testExportSessionServerError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("{\"msg\":\"Internal Server Error\"}"))

        val data = SessionDTO(
            id = "",
            installationId = "",
            createdAt = 123456789,
        )

        val res = internalHttpClient.exportSession(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Error.ServerError)

        val body = (res as HttpResponse.Error.ServerError).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Internal Server Error\"}")
    }

    @Test
    fun markSessionCrashed() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"msg\":\"Success\"}"))

        val res = internalHttpClient.markSessionCrashed("")
        assertTrue("export failed unexpectedly", res is HttpResponse.Success)

        val body = (res as HttpResponse.Success).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Success\"}")
    }

    @Test
    fun markSessionCrashedServerError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("{\"msg\":\"Internal Server Error\"}"))

        val res = internalHttpClient.markSessionCrashed("")
        assertTrue("export failed unexpectedly", res is HttpResponse.Error.ServerError)

        val body = (res as HttpResponse.Error.ServerError).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Internal Server Error\"}")
    }

    @Test
    fun exportInstallation() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"msg\":\"Success\"}"))

        val data = InstallationDTO(
            id = "",
            data = InstallationDataDTO(32, "", ""),
            createdAt = 123456789,
        )

        val res = internalHttpClient.exportInstallation(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Success)

        val body = (res as HttpResponse.Success).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Success\"}")
    }

    @Test
    fun exportInstallationServerError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("{\"msg\":\"Internal Server Error\"}"))

        val data = InstallationDTO(
            id = "",
            data = InstallationDataDTO(32, "", ""),
            createdAt = 123456789,
        )

        val res = internalHttpClient.exportInstallation(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Error.ServerError)

        val body = (res as HttpResponse.Error.ServerError).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Internal Server Error\"}")
    }

    @Test
    fun exportMemoryUsage() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody("{\"msg\":\"Success\"}"))

        val data = listOf(
            MemoryUsageDTO(
                id = "",
                sessionId = "",
                installationId = "",
                freeMemory = 1,
                usedMemory = 2,
                totalMemory = 3,
                maxMemory = 4,
                availableHeapSpace = 1,
                createdAt = 123456789
            )
        )

        val res = internalHttpClient.exportMemoryUsage(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Success)

        val body = (res as HttpResponse.Success).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Success\"}")
    }

    @Test
    fun exportMemoryUsageServerError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("{\"msg\":\"Internal Server Error\"}"))

        val data = listOf(
            MemoryUsageDTO(
                id = "",
                sessionId = "",
                installationId = "",
                freeMemory = 1,
                usedMemory = 2,
                totalMemory = 3,
                maxMemory = 4,
                availableHeapSpace = 1,
                createdAt = 123456789
            )
        )

        val res = internalHttpClient.exportMemoryUsage(data)
        assertTrue("export failed unexpectedly", res is HttpResponse.Error.ServerError)

        val body = (res as HttpResponse.Error.ServerError).body
        assertNotNull("Response body was null", body)
        assertEquals("Response body did not match expected", body, "{\"msg\":\"Internal Server Error\"}")
    }

}