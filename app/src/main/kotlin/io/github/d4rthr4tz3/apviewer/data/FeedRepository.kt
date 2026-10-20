package io.github.d4rthr4tz3.apviewer.data

import io.github.d4rthr4tz3.apviewer.data.db.FeedDao
import io.github.d4rthr4tz3.apviewer.data.db.FeedEntryEntity
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.store.StoreCodec
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Feed history per saved multiworld. */
class FeedRepository(private val dao: FeedDao) {
    /** Stores [entries] of the multiworld with [multiworldId]. */
    suspend fun append(multiworldId: String, entries: List<FeedEntry>) {
        if (entries.isEmpty()) return
        dao.insert(
            entries.map { entry ->
                FeedEntryEntity(
                    multiworldId = multiworldId,
                    receivedAtMillis = entry.receivedAtMillis,
                    type = entry.message.type,
                    highlight = entry.message.highlight.name,
                    messageJson = StoreCodec.encodeFeedMessage(entry.message),
                )
            },
        )
    }

    /** Stored messages of the multiworld with [multiworldId], oldest first. */
    fun observe(multiworldId: String): Flow<List<FeedEntry>> =
        dao.observe(multiworldId).map { entities ->
            entities.map { entity ->
                FeedEntry(
                    id = entity.id,
                    message = StoreCodec.decodeFeedMessage(entity.messageJson),
                    receivedAtMillis = entity.receivedAtMillis,
                )
            }
        }

    /** Deletes messages older than [retentionDays] days before [nowMillis]. */
    suspend fun prune(retentionDays: Int, nowMillis: Long = System.currentTimeMillis()) {
        dao.deleteOlderThan(nowMillis - TimeUnit.DAYS.toMillis(retentionDays.toLong()))
    }

    suspend fun deleteMultiworld(multiworldId: String) = dao.deleteMultiworld(multiworldId)
}
