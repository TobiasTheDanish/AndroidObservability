package dk.tobiasthedanish.observability.http

import android.util.Log
import dk.tobiasthedanish.observability.utils.ConfigService
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
}

private const val TAG = "InternalHttpClientImpl"

internal class InternalHttpClientImpl(
    private val client: HttpClient,
    private val env: ConfigService,
): InternalHttpClient {
    override suspend fun exportCollection(collection: ExportDTO): HttpResponse {
        try {
            Log.d(TAG, "Start export collection. URL: '${env.baseUrl}/api/v1/collection'")
            val res = client.post("${env.baseUrl}/api/v1/collection") {
                headers {
                    bearerAuth(env.apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(collection)
            }

            val body = res.bodyAsText()

            Log.d(TAG, "exportCollection response body: $body")
            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201, 202 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception thrown when exporting collection: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun exportSession(session: SessionDTO): HttpResponse {
        try {
            Log.d(TAG, "Start export session. URL: '${env.baseUrl}/api/v1/sessions'")
            val res = client.post("${env.baseUrl}/api/v1/sessions") {
                headers {
                    bearerAuth(env.apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(session)
            }

            val body = res.bodyAsText()

            Log.d(TAG, "exportSession response body: $body")
            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception thrown when exporting sessions: ${e.message}", e)
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
            Log.e(TAG, "Exception thrown when marking session as crashed: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun exportInstallation(installationDTO: InstallationDTO): HttpResponse {
        try {
            val res = client.post("${env.baseUrl}/api/v1/installations") {
                headers {
                    bearerAuth(env.apiKey)
                }
                contentType(ContentType.Application.Json)
                setBody(installationDTO)
            }

            val body = res.bodyAsText()

            return when (val status = res.status.value) {
                in (500..599) -> HttpResponse.Error.ServerError(status, body)
                in (400..499) -> HttpResponse.Error.ClientError(status, body)
                201 -> HttpResponse.Success(body)
                else -> HttpResponse.Error.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception thrown when exporting installation: ${e.message}", e)
            return HttpResponse.Error.UnknownError(e)
        }
    }
}
