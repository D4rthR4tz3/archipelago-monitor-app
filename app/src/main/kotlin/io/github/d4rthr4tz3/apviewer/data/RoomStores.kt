package io.github.d4rthr4tz3.apviewer.data

import io.github.d4rthr4tz3.apviewer.data.db.DataPackageDao
import io.github.d4rthr4tz3.apviewer.data.db.DataPackageEntity
import io.github.d4rthr4tz3.apviewer.data.db.ItemIndexDao
import io.github.d4rthr4tz3.apviewer.data.db.ItemIndexEntity
import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import io.github.d4rthr4tz3.apviewer.protocol.store.DataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.store.SlotKey
import io.github.d4rthr4tz3.apviewer.protocol.store.StoreCodec
import kotlinx.serialization.SerializationException

/** [ItemIndexStore] backed by Room, so items are reported once even across app restarts. */
class RoomItemIndexStore(private val dao: ItemIndexDao) : ItemIndexStore {
    override suspend fun lastReportedIndex(key: SlotKey): Int? =
        dao.lastIndex(key.server, key.seedName, key.team, key.slot)

    override suspend fun setLastReportedIndex(key: SlotKey, index: Int) {
        dao.upsert(ItemIndexEntity(key.server, key.seedName, key.team, key.slot, index))
    }
}

/** [DataPackageCache] backed by Room. */
class RoomDataPackageCache(private val dao: DataPackageDao) : DataPackageCache {
    override suspend fun get(game: String, checksum: String): GameData? {
        if (checksum.isEmpty()) return null
        val entity = dao.get(game)?.takeIf { it.checksum == checksum } ?: return null
        // A broken entry is treated as missing; the next download overwrites it.
        return try {
            StoreCodec.decodeGameData(entity.dataJson)
        } catch (e: SerializationException) {
            null
        }
    }

    override suspend fun put(game: String, data: GameData) {
        if (data.checksum.isNotEmpty()) {
            dao.upsert(DataPackageEntity(game, data.checksum, StoreCodec.encodeGameData(data)))
        }
    }
}
