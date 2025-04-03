package dk.tobiasthedanish.observability.http

import dk.tobiasthedanish.observability.utils.ConfigService
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

internal interface InternalHttpClient {
    suspend fun exportSession(session: SessionDTO): HttpResponse
    suspend fun markSessionCrashed(sessionId: String): HttpResponse
    suspend fun exportEvent(event: EventDTO): HttpResponse
    suspend fun exportTrace(trace: TraceDTO): HttpResponse
}

internal class InternalHttpClientImpl(
    private val client: HttpClient,
    private val env: ConfigService,
): InternalHttpClient {
    override suspend fun exportSession(session: SessionDTO): HttpResponse {
        try {
            val res = client.post("${env.baseUrl}/api/v1/sessions") {
                headers {
                    bearerAuth(env.apiKey)
                    setBody(session)
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
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun exportEvent(event: EventDTO): HttpResponse {
        try {
            val res = client.post("${env.baseUrl}/api/v1/events") {
                headers {
                    bearerAuth(env.apiKey)
                    setBody(event)
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
            return HttpResponse.Error.UnknownError(e)
        }
    }

    override suspend fun exportTrace(trace: TraceDTO): HttpResponse {
        try {
            val res = client.post("${env.baseUrl}/api/v1/traces") {
                headers {
                    bearerAuth(env.apiKey)
                    setBody(trace)
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
            return HttpResponse.Error.UnknownError(e)
        }
    }
}
