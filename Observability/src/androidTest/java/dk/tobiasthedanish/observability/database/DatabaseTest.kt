package dk.tobiasthedanish.observability.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import dk.tobiasthedanish.observability.events.EventTypes
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEvent
import dk.tobiasthedanish.observability.lifecycle.AppLifecycleEventType
import dk.tobiasthedanish.observability.utils.IdFactoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

    @Test
    fun testTraceCreation() = runBlocking {
        val parentTrace = runner.traceFactory.startTrace("ParentTrace")
        delay(200)

        val childTrace1 = runner.traceFactory.createTrace("Child1")
        childTrace1.setParent(parentTrace)
        childTrace1.start()
        delay(50)
        val childTrace2 = runner.traceFactory.createTrace("Child2")
        childTrace2.setParent(parentTrace)
        childTrace2.start()

        childTrace1.end()
        val fetchedChild1 = runner.getTrace(childTrace1.traceId)
        Assert.assertNotNull(fetchedChild1)
        Assert.assertEquals(childTrace1.traceId, fetchedChild1?.traceId)

        childTrace2.end()
        val fetchedChild2 = runner.getTrace(childTrace2.traceId)
        Assert.assertNotNull(fetchedChild2)
        Assert.assertEquals(childTrace2.traceId, fetchedChild2?.traceId)

        parentTrace.end()
        val fetchedParent = runner.getTrace(parentTrace.traceId)
        Assert.assertNotNull(fetchedParent)
        Assert.assertEquals(parentTrace.traceId, fetchedParent?.traceId)


        Assert.assertEquals(fetchedParent?.groupId, fetchedChild1?.groupId)
        Assert.assertEquals(fetchedParent?.groupId, fetchedChild2?.groupId)
    }

    @Test
    fun testMultipleEvents() {
        val (failed, entities) = runner.insertEvents(listOf(
            TestEvent(EventTypes.LIFECYCLE_APP, "", IdFactoryImpl().uuid()),
            TestEvent(EventTypes.LIFECYCLE_APP, ""),
            TestEvent(EventTypes.LIFECYCLE_APP, ""),
        ))

        Assert.assertEquals(1, failed)
        entities.forEachIndexed { i, entity ->
            val fetchedEntity = runner.getEvent(entity.id)
            if (i == 0) {
                Assert.assertNull(fetchedEntity)
            } else {
                Assert.assertEquals(entity.id, fetchedEntity?.id)
                Assert.assertEquals(entity.createdAt, fetchedEntity?.createdAt)
                Assert.assertEquals(entity.sessionId, fetchedEntity?.sessionId)
                Assert.assertEquals(entity.serializedData, fetchedEntity?.serializedData)
                Assert.assertEquals(entity.type, fetchedEntity?.type)
            }
        }
    }

    @Test
    fun testCreateMemoryUsage() {
        val createdEntity = runner.createMemoryUsage()
        val fetchedEntity = runner.getMemoryUsage(createdEntity.id)

        Assert.assertNotNull(fetchedEntity)
        Assert.assertEquals(createdEntity.id, fetchedEntity?.id)
        Assert.assertEquals(createdEntity.sessionId, fetchedEntity?.sessionId)
        Assert.assertEquals(createdEntity.usedMemory, fetchedEntity?.usedMemory)
        Assert.assertEquals(createdEntity.freeMemory, fetchedEntity?.freeMemory)
        Assert.assertEquals(createdEntity.maxMemory, fetchedEntity?.maxMemory)
        Assert.assertEquals(createdEntity.totalMemory, fetchedEntity?.totalMemory)
        Assert.assertEquals(createdEntity.availableHeapSpace, fetchedEntity?.availableHeapSpace)
        Assert.assertEquals(createdEntity.exported, fetchedEntity?.exported)
    }

    @Test
    fun insertMultipleMemoryUsages() {
        val (entities, failed) = runner.insertMemoryUsages(listOf(
            "Unknown session id",
            null,
            null
        ))

        Assert.assertEquals(1, failed)
        entities.forEachIndexed { i, createdEntity ->
            val fetchedEntity = runner.getMemoryUsage(createdEntity.id)
            if (i == 0) {
                Assert.assertNull(fetchedEntity)
            } else {
                Assert.assertNotNull(fetchedEntity)
                Assert.assertEquals(createdEntity.id, fetchedEntity?.id)
                Assert.assertEquals(createdEntity.sessionId, fetchedEntity?.sessionId)
                Assert.assertEquals(createdEntity.usedMemory, fetchedEntity?.usedMemory)
                Assert.assertEquals(createdEntity.freeMemory, fetchedEntity?.freeMemory)
                Assert.assertEquals(createdEntity.maxMemory, fetchedEntity?.maxMemory)
                Assert.assertEquals(createdEntity.totalMemory, fetchedEntity?.totalMemory)
                Assert.assertEquals(createdEntity.availableHeapSpace, fetchedEntity?.availableHeapSpace)
                Assert.assertEquals(createdEntity.exported, fetchedEntity?.exported)
            }
        }
    }
}