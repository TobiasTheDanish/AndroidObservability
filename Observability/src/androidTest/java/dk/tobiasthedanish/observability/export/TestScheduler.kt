package dk.tobiasthedanish.observability.export

import dk.tobiasthedanish.observability.scheduling.Scheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import java.util.concurrent.Callable
import java.util.concurrent.Future
import kotlin.time.Duration

class TestScheduler(
    private val testScope: TestScope = TestScope()
): Scheduler {
    fun advanceTimeBy(time: Duration) {
        testScope.testScheduler.advanceTimeBy(time)
    }

    override fun <T> start(callable: Callable<T>): Future<T> {
        val job = testScope.async {
            callable.call()
        }

        return job.asCompletableFuture()
    }

    override fun <T> start(block: suspend () -> T): Future<T> {
        val job = testScope.async {
            block()
        }

        return job.asCompletableFuture()
    }

    override fun <T> schedule(callable: Callable<T>, delayMillis: Long): Future<T> {
        val job = testScope.async {
            delay(delayMillis)
            callable.call()
        }

        return job.asCompletableFuture()
    }

    override fun scheduleAtRate(
        runnable: Runnable,
        rateMillis: Long,
        initialDelayMillis: Long
    ): Future<*> {
        val job = testScope.async {
            delay(initialDelayMillis)
            while (this.isActive) {
                runnable.run()
                delay(rateMillis)
            }
        }

        return job.asCompletableFuture()
    }
}