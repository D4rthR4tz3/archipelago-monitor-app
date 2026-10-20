package io.github.d4rthr4tz3.apviewer.protocol.store

import java.util.concurrent.ConcurrentHashMap

/** Identifies a slot across connections: the same server, seed, team and slot number. */
data class SlotKey(val server: String, val seedName: String, val team: Int, val slot: Int)

/**
 * Remembers the index of the last received item that was already reported per slot, so that items
 * are reported only once, even across reconnects and app restarts.
 */
interface ItemIndexStore {
    /** Returns the last reported index of [key], `-1` if none was reported yet, or `null` if the
     * slot was never seen. */
    suspend fun lastReportedIndex(key: SlotKey): Int?

    /** Stores [index] as the last reported index of [key]. */
    suspend fun setLastReportedIndex(key: SlotKey, index: Int)
}

/** [ItemIndexStore] that lives as long as the process. */
class InMemoryItemIndexStore : ItemIndexStore {
    private val indices = ConcurrentHashMap<SlotKey, Int>()

    override suspend fun lastReportedIndex(key: SlotKey): Int? = indices[key]

    override suspend fun setLastReportedIndex(key: SlotKey, index: Int) {
        indices[key] = index
    }
}
