package dk.tobiasthedanish.observability.session

import android.util.Log
import dk.tobiasthedanish.observability.events.Event
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleListener
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.SessionEntity
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.isUnhandledException
import kotlinx.coroutines.runBlocking

internal interface SessionManager: AppLifecycleListener {
    fun init()
    fun getSessionId(): String
    fun <T : Any> onEventTracked(event: Event<T>)
}

private const val SESSION_MAX_DURATION_MS = 21600000
private const val SESSION_MAX_TIME_SINCE_EVENT = 1200000
private const val TAG = "SessionManagerImpl"

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

        currentSession = when {
            recentSession != null && continueSession(recentSession) -> {
                Log.d(TAG, "Reusing session with id: ${recentSession.id}")
                recentSession
            }
            else -> createNewSession()
        }
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

    private fun continueSession(session: Session): Boolean {
        if (session.crashed) {
            Log.d(TAG, "Recent session crashed. DONT continue this session")
            return false
        }

        val sessionDuration = timeProvider.now() - session.createdAt
        if (SESSION_MAX_DURATION_MS < sessionDuration || sessionDuration < 0) {
            Log.d(TAG, "Duration of session (${sessionDuration}MS) exceeded max duration ($SESSION_MAX_DURATION_MS). DONT continue this session")
            return false
        }

        if (session.lastEventTime > 0) {
            val timeSinceLastEvent = timeProvider.now() - session.lastEventTime
            Log.d(TAG, "Last event time: ${session.lastEventTime}, Time since last event: $timeSinceLastEvent, max time since last event: ${SESSION_MAX_TIME_SINCE_EVENT}. If true continue this session: ${SESSION_MAX_TIME_SINCE_EVENT < timeSinceLastEvent || timeSinceLastEvent < 0}")
            return timeSinceLastEvent in 0..<SESSION_MAX_TIME_SINCE_EVENT
        }

        return true
    }

    private fun createNewSession(): Session {
        Log.d(TAG, "Creating new session")
        val newSession = Session(
            id = idFactory.uuid(),
            createdAt = timeProvider.now()
        )

        sessionStore.setRecent(newSession)
        db.createSession(session = SessionEntity(newSession.id, newSession.createdAt, newSession.crashed))
        return newSession
    }
}
