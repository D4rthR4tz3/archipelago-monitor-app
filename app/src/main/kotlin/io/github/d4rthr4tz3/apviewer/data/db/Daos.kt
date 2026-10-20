package io.github.d4rthr4tz3.apviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Insert
    suspend fun insert(entries: List<FeedEntryEntity>)

    @Query(
        "SELECT * FROM (SELECT * FROM feed_entries WHERE multiworld_id = :multiworldId " +
            "ORDER BY id DESC LIMIT :limit) ORDER BY id",
    )
    fun observeLatest(multiworldId: String, limit: Int): Flow<List<FeedEntryEntity>>

    @Query(
        "SELECT COUNT(*) FROM feed_entries WHERE multiworld_id = :multiworldId " +
            "AND highlight = 'FOR_ME' AND id > :afterId",
    )
    fun observeCountForMe(multiworldId: String, afterId: Long): Flow<Int>

    @Query(
        "SELECT * FROM feed_entries WHERE multiworld_id = :multiworldId " +
            "AND highlight = 'FOR_ME' ORDER BY id DESC LIMIT 1",
    )
    fun observeLatestForMe(multiworldId: String): Flow<FeedEntryEntity?>

    @Query("SELECT MAX(id) FROM feed_entries WHERE multiworld_id = :multiworldId")
    fun observeLatestId(multiworldId: String): Flow<Long?>

    @Query(
        "DELETE FROM feed_entries WHERE multiworld_id = :multiworldId " +
            "AND received_at < :cutoffMillis",
    )
    suspend fun deleteOlderThan(multiworldId: String, cutoffMillis: Long): Int

    @Query("DELETE FROM feed_entries WHERE multiworld_id = :multiworldId")
    suspend fun deleteMultiworld(multiworldId: String)
}

@Dao
interface ItemIndexDao {
    @Query(
        "SELECT last_index FROM item_indices " +
            "WHERE server = :server AND seed_name = :seedName AND team = :team AND slot = :slot",
    )
    suspend fun lastIndex(server: String, seedName: String, team: Int, slot: Int): Int?

    @Upsert
    suspend fun upsert(entity: ItemIndexEntity)
}

@Dao
interface DataPackageDao {
    @Query("SELECT * FROM data_packages WHERE game = :game")
    suspend fun get(game: String): DataPackageEntity?

    @Upsert
    suspend fun upsert(entity: DataPackageEntity)
}
