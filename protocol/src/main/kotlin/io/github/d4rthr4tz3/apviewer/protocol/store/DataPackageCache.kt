package io.github.d4rthr4tz3.apviewer.protocol.store

import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache of [GameData] per game, so that names do not have to be downloaded on every connection.
 * Entries are only valid for the checksum the server reports in `RoomInfo`.
 */
interface DataPackageCache {
    /** Returns the cached data of [game] if its checksum equals [checksum], else `null`. */
    suspend fun get(game: String, checksum: String): GameData?

    /** Stores [data] for [game]. Data without a checksum is not cached. */
    suspend fun put(game: String, data: GameData)
}

/** [DataPackageCache] that lives as long as the process. */
class InMemoryDataPackageCache : DataPackageCache {
    private val games = ConcurrentHashMap<String, GameData>()

    override suspend fun get(game: String, checksum: String): GameData? =
        games[game]?.takeIf { checksum.isNotEmpty() && it.checksum == checksum }

    override suspend fun put(game: String, data: GameData) {
        if (data.checksum.isNotEmpty()) {
            games[game] = data
        }
    }
}
