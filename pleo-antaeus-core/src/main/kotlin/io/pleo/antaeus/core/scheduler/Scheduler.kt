package io.pleo.antaeus.core.scheduler

import com.github.shyiko.skedule.Schedule
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Scheduler(corePoolSize: Int = 1) {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(corePoolSize)
    private var activeTask: ScheduledFuture<*>? = null

    fun scheduleTask(task: () -> Unit, day: String = "1", hour: String = "00", minutes: String = "00") {
        val now = ZonedDateTime.now()
        executor.removeOnCancelPolicy = true

        activeTask = executor.schedule(task, Schedule.parse("$day of month $hour:$minutes").iterate(now).next().toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
    }

    fun stopActiveTask() {
        activeTask?.cancel(true)
        activeTask = null
    }

    fun isTaskActive(): Boolean = activeTask?.let { true } ?: false
}