package dk.tobiasthedanish.observability.exception

import kotlinx.serialization.Serializable

@Serializable
internal data class ExceptionUnit(
    val name: String,
    val message: String?,
    val frames: List<StackFrameUnit>
)

@Serializable
internal data class StackFrameUnit(
    val className: String? = null,
    val methodName: String? = null,
    val fileName: String? = null,
    val lineNum: Int? = null,
)