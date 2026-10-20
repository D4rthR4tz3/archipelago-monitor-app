package io.github.d4rthr4tz3.apviewer.protocol.session

/** Exponential backoff between reconnect attempts, capped at [maxDelayMillis]. */
data class ReconnectPolicy(
    val initialDelayMillis: Long = 1_000,
    val maxDelayMillis: Long = 60_000,
    val factor: Double = 2.0,
) {
    /** Returns the delay before reconnect attempt [attempt], counting from 1. */
    fun delayMillis(attempt: Int): Long {
        var delay = initialDelayMillis.toDouble()
        repeat(attempt - 1) { delay = minOf(delay * factor, maxDelayMillis.toDouble()) }
        return delay.toLong().coerceAtMost(maxDelayMillis)
    }
}
