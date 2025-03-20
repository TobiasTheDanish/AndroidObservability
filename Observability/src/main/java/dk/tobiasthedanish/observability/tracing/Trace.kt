package dk.tobiasthedanish.observability.tracing

interface Trace {
    val groupId: String // i'm not sure about this
    val traceId: String
    val parentId: String?
    val name: String
    val status: TraceStatus

    fun setParent(parent: Trace): Trace
    fun setStatus(status: TraceStatus): Trace

    fun start()
    fun end()
    fun hasEnded(): Boolean
}

sealed class TraceStatus(val name: String) {
    data object Ok: TraceStatus("Ok")
    data class Error(val message: String): TraceStatus("Error")
}
