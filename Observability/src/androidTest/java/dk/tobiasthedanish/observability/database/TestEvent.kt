package dk.tobiasthedanish.observability.database

data class TestEvent(val type: String, val data: String, val sessionId: String? = null)
