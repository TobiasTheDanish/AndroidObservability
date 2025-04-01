package dk.tobiasthedanish.observability.http

sealed class HttpResponse {
    data class Success(val body: String? = null): HttpResponse()
    sealed class Error: HttpResponse() {
        data class ClientError(val code: Int, val body: String? = null) : Error()
        data class ServerError(val code: Int, val body: String? = null) : Error()
        data class UnknownError(val exception: Exception? = null) : Error()
    }
}