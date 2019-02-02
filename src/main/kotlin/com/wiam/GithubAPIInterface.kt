package com.wiam

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToLong

const val RATELIMIT_REMAINING = "X-RateLimit-Remaining"
const val RATELIMIT_RESET_TIME = "X-RateLimit-Reset"

class RequestError(val code: Int) : Exception("Request failed with code $code")

class GithubAPIInterface(
    private val cnx: ConnectionFactory = ConnectionFactory(),
    private val timer: DefaultTimer = DefaultTimer()
) {
    private val lock = ReentrantLock()

    var nextCall = Instant.now()

    fun call(url: URL): URLConnection {
        lock.withLock {
            log.fine("Waiting ${nextCall.epochSecond - Instant.now().epochSecond} before the next call")
            timer.waitUntil(nextCall)
            return innerCall(url)
        }
    }

    private fun innerCall(url: URL, isRetry: Boolean = false): HttpURLConnection {
        val connection = cnx.connect(url)
        val remaining = connection.getHeaderField(RATELIMIT_REMAINING).toInt()
        val resetTime = Instant.ofEpochSecond(connection.getHeaderField(RATELIMIT_RESET_TIME).toLong())
        log.fine("Rate limit: $remaining calls left until $resetTime s.")

        when (connection.responseCode) {
            200 -> {
                nextCall = timer.futureTime(
                    (resetTime.epochSecond - Instant.now().epochSecond).toDouble()
                        .div(remaining.toDouble()).roundToLong() + 1
                )
                return connection
            }
            403 -> {
                if (remaining == 0 && !isRetry) {
                    log.warning("We have been rate limited, limit lifted at $resetTime. Waiting")
                    nextCall = resetTime
                    return innerCall(url, true)
                } else {
                    throw RequestError(403)
                }
            }
            else -> throw RequestError(connection.responseCode)
        }
    }
}
