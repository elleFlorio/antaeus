package io.pleo.antaeus.core.scheduler

import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SchedulerTest {

    private val scheduler = Scheduler()
    private var done: AtomicBoolean = AtomicBoolean(false)
    private val task: () -> Unit = {
        done.set(true)
    }

    @Test
    fun `Will correctly schedule and execute a task`() {
        val now = ZonedDateTime.now()
        val next = now.plusMinutes(1)

        val hourFormatter = DateTimeFormatter.ofPattern("HH")
        val minutesFormatter = DateTimeFormatter.ofPattern("mm")

        val day = next.dayOfMonth.toString()
        val hour = next.format(hourFormatter)
        val minutes = next.format(minutesFormatter)

        scheduler.scheduleTask(task, day, hour, minutes)
        Assertions.assertTrue(scheduler.isTaskActive())
        await().atMost(2, TimeUnit.MINUTES).untilTrue(done)
    }
}