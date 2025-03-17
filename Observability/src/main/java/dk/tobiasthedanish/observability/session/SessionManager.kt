package dk.tobiasthedanish.observability.session

import dk.tobiasthedanish.observability.events.Event
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.exception.ExceptionEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleListener
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.SessionEntity
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.IdFactory
import kotlinx.coroutines.runBlocking

internal interface SessionManager: AppLifecycleListener {
    fun init()
    fun getSessionId(): String
    fun <T : Any> onEventTracked(event: Event<T>)
}

internal class SessionManagerImpl(
    private val timeProvider: TimeProvider,
    private val idFactory: IdFactory,
    private val sessionStore: SessionStore,
    private val db: Database,
): SessionManager {
    private var currentSession: Session? = null
    private var appBackgroundTime: Long = 0

    override fun init() = runBlocking {
        val recentSession = sessionStore.getRecent()

        currentSession = recentSession ?: createNewSession()
    }

    override fun getSessionId(): String {
        val sessionId = this.currentSession?.id
        requireNotNull(sessionId) {
            "Session manager must be initialized before accessing current session id"
        }
        return sessionId
    }

    override fun <T : Any> onEventTracked(event: Event<T>) {
        val sessionId = this.currentSession?.id ?: return

        if (event.sessionId != sessionId)
            return

        sessionStore.updateLastEventTime(event.timestamp)

        if (event.isUnhandledException()) {
            sessionStore.setSessionCrashed()
        }
    }

    override fun onAppForeground() {
        if (appBackgroundTime == 0L) {
            // app hasn't gone to background yet, it's coming to foreground for the first time.
            return
        }
        appBackgroundTime = 0
        // re-initialize session if needed
        init()
    }

    override fun onAppBackground() {
        appBackgroundTime = timeProvider.elapsedRealtime
    }

    private fun createNewSession(): Session {
        val newSession = Session(
            id = idFactory.uuid(),
            createdAt = timeProvider.now()
        )

        sessionStore.setRecent(newSession)
        db.createSession(session = SessionEntity(newSession.id, newSession.createdAt, newSession.crashed))
        return newSession
    }

    private fun <T:Any> Event<T>.isUnhandledException(): Boolean {
        return this.type == EventTypes.UNHANDLED_EXCEPTION && this.data is ExceptionEvent && !this.data.handled
    }
}
