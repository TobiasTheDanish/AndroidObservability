package dk.tobiasthedanish.observability.exception

private const val MAX_FRAMES_IN_STACKTRACE = 64
private const val MAX_CONSECUTIVE_FRAME_REPEATS = 3

internal fun Array<StackTraceElement>.trim(
    maxRepeats: Int = MAX_CONSECUTIVE_FRAME_REPEATS,
    maxSize: Int = MAX_FRAMES_IN_STACKTRACE,
): Array<StackTraceElement> {
    val result = mutableListOf<StackTraceElement>()
    var currentElement: StackTraceElement? = null
    var currentCount = 0

    for (element in this) {
        if (element == currentElement) {
            currentCount++
            if (currentCount <= maxRepeats) {
                result.add(element)
            }
        } else {
            currentElement = element
            currentCount = 1
            result.add(element)
        }
    }

    // Check if the result list is larger than maxSize
    if (result.size > maxSize) {
        val middleIndex = result.size / 2
        val startIndex = middleIndex - maxSize / 2
        val endIndex = middleIndex + maxSize / 2
        result.subList(startIndex, endIndex).clear()
    }

    return result.toTypedArray()
}
