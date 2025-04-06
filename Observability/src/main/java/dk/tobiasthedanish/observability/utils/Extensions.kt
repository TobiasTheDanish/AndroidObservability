package dk.tobiasthedanish.observability.utils

import dk.tobiasthedanish.observability.events.Event
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.exception.ExceptionEvent

internal fun <T:Any> Event<T>.isUnhandledException(): Boolean {
    return this.type == EventTypes.UNHANDLED_EXCEPTION && this.data is ExceptionEvent && !this.data.handled
}
