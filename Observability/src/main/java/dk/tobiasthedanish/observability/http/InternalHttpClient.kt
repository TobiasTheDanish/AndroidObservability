package dk.tobiasthedanish.observability.http

import dk.tobiasthedanish.observability.utils.ConfigService
import dk.tobiasthedanish.observability.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal interface InternalHttpClient {
    suspend fun exportCollection(collection: ExportDTO): HttpResponse
    suspend fun exportSession(session: SessionDTO): HttpResponse
    suspend fun markSessionCrashed(sessionId: String): HttpResponse
    suspend fun exportInstallation(installationDTO: InstallationDTO): HttpResponse
    suspend fun exportMemoryUsage(memoryUsage: List<MemoryUsageDTO>): HttpResponse
}

private const val TAG = "InternalHttpClientImpl"

internal class InternalHttpClientImpl(
    private val client: HttpClient,
    private val env: ConfigService,
    private val logger: Logger = Logger(TAG),
): InternalHttpClient {
    override suspend fun exportCollection(collection: ExportDTO): HttpResponse {
        try {
            logger.debug("Start export collection. URL: '${env.baseUrl}/api/v1/collection'")
            val res = client.post("${env.baseUrl}/api/v1/collection") {
                headers {
                    bearerAuth(env.apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(collection)
            }

            val body = res.bodyAsText()

            logger.debug("exportCollection response body: $body")
            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201, 202 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            logger.error("Exception thrown when exporting collection: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun exportSession(session: SessionDTO): HttpResponse {
        try {
            logger.debug("Start export session. URL: '${env.baseUrl}/api/v1/sessions'")
            val res = client.post("${env.baseUrl}/api/v1/sessions") {
                headers {
                    bearerAuth(env.apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(session)
            }

            val body = res.bodyAsText()

            logger.debug("exportSession response body: $body")
            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            logger.error("Exception thrown when exporting sessions: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun markSessionCrashed(sessionId: String): HttpResponse {
        try {
            val res = client.post("${env.baseUrl}/api/v1/sessions/$sessionId/crash") {
                headers {
                    bearerAuth(env.apiKey)
                }
            }

            val body = res.bodyAsText()

            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            logger.error("Exception thrown when marking session as crashed: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun exportInstallation(installationDTO: InstallationDTO): HttpResponse {
        try {
            logger.debug("Installation to export: $installationDTO")
            val res = client.post("${env.baseUrl}/api/v1/installations/android") {
                headers {
                    bearerAuth(env.apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(installationDTO)
            }

            val body = res.bodyAsText()

            logger.debug("exportInstallation response body: $body")
            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            logger.error("Exception thrown when exporting installation: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun exportMemoryUsage(memoryUsage: List<MemoryUsageDTO>): HttpResponse {
        try {
            logger.debug("MemoryUsage to export: $memoryUsage")
            val res = client.post("${env.baseUrl}/api/v1/resources/memory") {
                headers {
                    bearerAuth(env.apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(memoryUsage)
            }

            val body = res.bodyAsText()

            logger.debug("exportMemoryUsage response body: $body")
            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            logger.error("Exception thrown when exporting memory usage: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }
}
