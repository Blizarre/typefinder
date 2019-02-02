package com.wiam.github

import com.wiam.log
import java.time.Duration
import java.time.Instant

class DefaultTimer {
    private fun currentTime(): Instant {
        return Instant.now()
    }

    fun waitUntil(i: Instant) {
        val waitingTime = Duration.between(currentTime(), i)
        if (waitingTime.isNegative) {
            log.info("Not waiting")
            return
        }

        log.info("Waiting for ${waitingTime.toMillis()} ms.")
        Thread.sleep(waitingTime.toMillis())
    }

    fun futureTime(secondInFuture: Long): Instant {
        return Instant.now().plus(Duration.ofSeconds(secondInFuture))
    }
}
