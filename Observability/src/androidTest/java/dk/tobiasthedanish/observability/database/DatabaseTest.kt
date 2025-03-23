package dk.tobiasthedanish.observability.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEventType
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class DatabaseTest {
    private val runner = DatabaseTestRunner()

    @Before
    fun setup() {
        runner.setup()
    }

    @After
    fun teardown() {
        runner.clearData()
    }

    @Test
    fun testSessionCreation() {
        val createdEntity = runner.createSession()
        val fetchedEntity = runner.getSession(createdEntity.id)

        Assert.assertNotNull(fetchedEntity)

        Assert.assertEquals(createdEntity.id, fetchedEntity?.id)
        Assert.assertEquals(createdEntity.createdAt, fetchedEntity?.createdAt)
        Assert.assertEquals(createdEntity.crashed, fetchedEntity?.crashed)
    }

    @Test
    fun testEventCreation() {
        val createdEntity = runner.createEvent(EventTypes.LIFECYCLE_APP, Json.encodeToString(AppLifecycleEvent(AppLifecycleEventType.FOREGROUND)))
        val fetchedEntity = runner.getEvent(createdEntity.id)

        Assert.assertNotNull(fetchedEntity)

        Assert.assertEquals(createdEntity.id, fetchedEntity?.id)
        Assert.assertEquals(createdEntity.createdAt, fetchedEntity?.createdAt)
        Assert.assertEquals(createdEntity.sessionId, fetchedEntity?.sessionId)
        Assert.assertEquals(createdEntity.serializedData, fetchedEntity?.serializedData)
        Assert.assertEquals(createdEntity.type, fetchedEntity?.type)
    }

    @Test
    fun testEventWithUnknownSessionId() {
        val createdEntity = runner.createEvent(
            EventTypes.LIFECYCLE_APP,
            Json.encodeToString(AppLifecycleEvent(AppLifecycleEventType.FOREGROUND)),
            IdFactoryImpl().uuid()
        )
        val fetchedEntity = runner.getEvent(createdEntity.id)

        Assert.assertNull(fetchedEntity)
    }

    @Test
    fun testSessionCrashed() {
        runner.setSessionCrashed(sessionId = runner.sessionId)

        val session = runner.getSession(runner.sessionId)

        Assert.assertNotNull(session)
        Assert.assertEquals(true, session?.crashed)
        Assert.assertEquals(runner.sessionId, session?.id)
    }

    @Test
    fun testSessionExported() {
        runner.setSessionExported(sessionId = runner.sessionId)

        val session = runner.getSession(runner.sessionId)

        Assert.assertNotNull(session)
        Assert.assertEquals(true, session?.exported)
        Assert.assertEquals(runner.sessionId, session?.id)
    }

    @Test
    fun testDeleteExportedSessions() {
        val createdEntity = runner.createSession()

        runner.setSessionExported(sessionId = createdEntity.id)

        runner.deleteExportedSessions()

        val session = runner.getSession(createdEntity.id)

        Assert.assertNull(session)
    }
}