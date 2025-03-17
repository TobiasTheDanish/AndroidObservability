package dk.tobiasthedanish.observability.utils

import java.util.UUID

internal interface IdFactory {
    fun uuid(): String
}

internal class IdFactoryImpl:IdFactory {
    override fun uuid(): String {
        return UUID.randomUUID().toString()
    }
}
