package dk.tobiasthedanish.observability.exception

import kotlinx.serialization.Serializable

@Serializable
internal data class ExceptionEvent(
    val threadName: String,
    val handled: Boolean,
    val exceptionUnits: List<ExceptionUnit>
)

internal class ExceptionEventFactory {
    companion object {
        fun create(
            thread: Thread,
            throwable: Throwable,
            handled: Boolean = false
        ): ExceptionEvent {
            val exceptionUnits = mutableListOf<ExceptionUnit>()

            var error: Throwable? = throwable
            while (error != null) {
                val frames = error.stackTrace.trim().map {
                    StackFrameUnit(
                        className = it.className,
                        fileName = it.fileName,
                        methodName = it.methodName,
                        lineNum = it.lineNumber,
                    )
                }
                exceptionUnits.add(
                    ExceptionUnit(
                        frames = frames,
                        message = error.message,
                        name = error.javaClass.name,
                    )
                )

                error = error.cause
            }

            return ExceptionEvent(
                threadName = thread.name,
                exceptionUnits = exceptionUnits,
                handled = handled,
            )
        }
    }
}