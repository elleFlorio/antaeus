package io.pleo.antaeus.core.scheduler

import com.github.shyiko.skedule.Schedule
import java.time.ZonedDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Scheduler(corePoolSize: Int = 1) {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(corePoolSize)

    fun scheduleTask(task: () -> Unit, hour: String, minutes: String) {
        val now = ZonedDateTime.now()
        executor.removeOnCancelPolicy = true

        executor.schedule(task, Schedule.parse("1 of month $hour:$minutes").iterate(now).next().toEpochSecond() - now.toEpochSecond(), TimeUnit.SECONDS)
    }
}