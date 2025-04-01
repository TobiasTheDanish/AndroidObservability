package dk.tobiasthedanish.observability.http

import dk.tobiasthedanish.observability.storage.EventEntity
import dk.tobiasthedanish.observability.storage.SessionEntity
import dk.tobiasthedanish.observability.storage.TraceEntity
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal object HttpClientFactory {
    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    val internalClient: InternalHttpClient by lazy { InternalHttpClientImpl(client) }
}

internal interface InternalHttpClient {
    suspend fun exportSession(session: SessionEntity): HttpResponse
    suspend fun exportEvent(event: EventEntity): HttpResponse
    suspend fun exportTrace(trace: TraceEntity): HttpResponse
}

internal class InternalHttpClientImpl(
    private val client: HttpClient
): InternalHttpClient {
    override suspend fun exportSession(session: SessionEntity): HttpResponse {
        TODO("Not yet implemented")
    }

    override suspend fun exportEvent(event: EventEntity): HttpResponse {
        TODO("Not yet implemented")
    }

    override suspend fun exportTrace(trace: TraceEntity): HttpResponse {
        TODO("Not yet implemented")
    }

}