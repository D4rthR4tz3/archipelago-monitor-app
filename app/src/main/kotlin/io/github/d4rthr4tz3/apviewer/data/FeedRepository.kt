package io.github.d4rthr4tz3.apviewer.data

import io.github.d4rthr4tz3.apviewer.data.db.FeedDao
import io.github.d4rthr4tz3.apviewer.data.db.FeedEntryEntity
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.session.FeedSink
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedMultiworld
import io.github.d4rthr4tz3.apviewer.protocol.store.StoreCodec
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Feed history per saved multiworld. */
class FeedRepository(private val dao: FeedDao) : FeedSink {
    /** Stores [entries] of the multiworld with [multiworldId]. */
    override suspend fun append(multiworldId: String, entries: List<FeedEntry>) {
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

    /** The newest [limit] stored messages of the multiworld with [multiworldId], oldest first. */
    fun observe(multiworldId: String, limit: Int = DEFAULT_LIMIT): Flow<List<FeedEntry>> =
        dao.observeLatest(multiworldId, limit).map { entities -> entities.map { it.toEntry() } }

    /** Number of items for the user's slots stored after [afterId]. */
    fun observeCountForMe(multiworldId: String, afterId: Long): Flow<Int> =
        dao.observeCountForMe(multiworldId, afterId)

    /** The newest stored item for one of the user's slots. */
    fun observeLatestForMe(multiworldId: String): Flow<FeedEntry?> =
        dao.observeLatestForMe(multiworldId).map { it?.toEntry() }

    /** Id of the newest stored message. */
    fun observeLatestId(multiworldId: String): Flow<Long?> = dao.observeLatestId(multiworldId)

    /** Deletes messages older than each multiworld's retention period before [nowMillis]. */
    suspend fun prune(
        multiworlds: List<SavedMultiworld>,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        multiworlds
            .filter { it.feedRetentionDays != SavedMultiworld.RETENTION_UNLIMITED }
            .forEach { multiworld ->
                val retention = TimeUnit.DAYS.toMillis(multiworld.feedRetentionDays.toLong())
                dao.deleteOlderThan(multiworld.id, nowMillis - retention)
            }
    }

    suspend fun deleteMultiworld(multiworldId: String) = dao.deleteMultiworld(multiworldId)

    private fun FeedEntryEntity.toEntry() = FeedEntry(
        id = id,
        message = StoreCodec.decodeFeedMessage(messageJson),
        receivedAtMillis = receivedAtMillis,
    )

    private companion object {
        const val DEFAULT_LIMIT = 1_000
    }
}
