package dk.tobiasthedanish.observability.scheduling

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal interface Scheduler {
    @Throws(RejectedExecutionException::class)
    fun <T> start(callable: Callable<T>): Future<T>
    @Throws(RejectedExecutionException::class)
    fun <T> start(block: suspend () -> T): Future<T>
    @Throws(RejectedExecutionException::class)
    fun <T> schedule(callable: Callable<T>, delayMillis: Long): Future<T>
    @Throws(RejectedExecutionException::class)
    fun scheduleAtRate(runnable: Runnable, rateMillis: Long, initialDelayMillis: Long = 0): Future<*>
}

private const val TAG = "SchedulerImpl"

internal class SchedulerImpl(
    private val executorService: ScheduledExecutorService,
    private val scope: CoroutineScope,
): Scheduler {
    override fun <T> start(callable: Callable<T>): Future<T> {
        Log.d(TAG, "start called with callable")
        return executorService.submit(callable)
    }

    override fun <T> start(block: suspend () -> T): Future<T> {
        Log.d(TAG, "start called with suspend block")
        return scope.async {
            Log.d(TAG, "Start of suspend block")
            val res = block()
            Log.d(TAG, "End of suspend block")
            res
        }.asCompletableFuture()
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
