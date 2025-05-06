package dk.tobiasthedanish.observability.session

import dk.tobiasthedanish.observability.events.Event
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleListener
import dk.tobiasthedanish.observability.storage.Database
import dk.tobiasthedanish.observability.storage.SessionEntity
import dk.tobiasthedanish.observability.time.TimeProvider
import dk.tobiasthedanish.observability.utils.ConfigService
import dk.tobiasthedanish.observability.utils.IdFactory
import dk.tobiasthedanish.observability.utils.Logger
import dk.tobiasthedanish.observability.utils.isUnhandledException
import kotlinx.coroutines.runBlocking

internal interface SessionManager: AppLifecycleListener {
    fun init()
    fun getSessionId(): String
    fun <T : Any> onEventTracked(event: Event<T>)
}

private const val TAG = "SessionManagerImpl"

internal class SessionManagerImpl(
    private val timeProvider: TimeProvider,
    private val idFactory: IdFactory,
    private val sessionStore: SessionStore,
    private val db: Database,
    private val configService: ConfigService,
    private val logger: Logger = Logger(TAG)
): SessionManager {
    private var currentSession: Session? = null
    private var appBackgroundTime: Long = 0

    override fun init() = runBlocking {
        val recentSession = sessionStore.getRecent()

        currentSession = when {
            recentSession != null && continueSession(recentSession) -> {
                logger.debug("Reusing session with id: ${recentSession.id}")
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
            logger.debug("Recent session crashed. DONT continue this session")
            return false
        }

        val sessionDuration = timeProvider.now() - session.createdAt
        if (configService.maxSessionDuration.inWholeMilliseconds < sessionDuration || sessionDuration < 0) {
            logger.debug("Duration of session (${sessionDuration}MS) exceeded max duration (${configService.maxSessionDuration.inWholeMilliseconds}). DONT continue this session")
            return false
        }

        if (session.lastEventTime > 0) {
            val timeSinceLastEvent = timeProvider.now() - session.lastEventTime
            logger.debug("Last event time: ${session.lastEventTime}, Time since last event: $timeSinceLastEvent, max time since last event: ${configService.maxSessionTimeBetweenEvents.inWholeMilliseconds}. If true continue this session: ${configService.maxSessionTimeBetweenEvents.inWholeMilliseconds < timeSinceLastEvent || timeSinceLastEvent < 0}")
            return timeSinceLastEvent in 0..<configService.maxSessionTimeBetweenEvents.inWholeMilliseconds
        }

        return true
    }

    private fun createNewSession(): Session {
        logger.debug("Creating new session")
        val newSession = Session(
            id = idFactory.uuid(),
            createdAt = timeProvider.now()
        )

        sessionStore.setRecent(newSession)
        db.createSession(session = SessionEntity(newSession.id, newSession.createdAt, newSession.crashed))
        return newSession
    }
}
