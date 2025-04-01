package dk.tobiasthedanish.observability.scheduling

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal interface Scheduler {
    @Throws(RejectedExecutionException::class)
    fun <T> start(callable: Callable<T>): Future<T>
    @Throws(RejectedExecutionException::class)
    fun <T> schedule(callable: Callable<T>, delayMillis: Long): Future<T>
    @Throws(RejectedExecutionException::class)
    fun scheduleAtRate(runnable: Runnable, rateMillis: Long, initialDelayMillis: Long = 0): Future<*>
}

internal class SchedulerImpl(
    private val executorService: ScheduledExecutorService
): Scheduler {
    override fun <T> start(callable: Callable<T>): Future<T> {
        return executorService.submit(callable)
    }

    override fun <T> schedule(callable: Callable<T>, delayMillis: Long): Future<T> {
        return executorService.schedule(callable, delayMillis, TimeUnit.MILLISECONDS)
    }

    override fun scheduleAtRate(
        runnable: Runnable,
        rateMillis: Long,
        initialDelayMillis: Long
    ): Future<*> {
        return executorService.scheduleWithFixedDelay(
            runnable,
            initialDelayMillis,
            rateMillis,
            TimeUnit.MILLISECONDS
        )
    }
}
