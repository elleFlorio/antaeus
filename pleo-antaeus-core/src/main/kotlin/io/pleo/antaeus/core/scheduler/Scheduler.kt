package io.pleo.antaeus.core.scheduler

import com.github.shyiko.skedule.Schedule
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Scheduler(corePoolSize: Int = 1) {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(corePoolSize)
    private var activeTask: ScheduledFuture<*>? = null

    fun scheduleTask(task: () -> Unit, hour: String, minutes: String) {
        val now = ZonedDateTime.now()
        executor.removeOnCancelPolicy = true

        activeTask = executor.schedule(task, Schedule.parse("1 of month $hour:$minutes").iterate(now).next().toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
    }

    fun stopActiveTask() = activeTask?.cancel(true)

    fun isTaskActive(): Boolean = activeTask?.let { true } ?: false
}