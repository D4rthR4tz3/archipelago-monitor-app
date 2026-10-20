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

    @Query("SELECT * FROM feed_entries WHERE multiworld_id = :multiworldId ORDER BY id")
    fun observe(multiworldId: String): Flow<List<FeedEntryEntity>>

    @Query("DELETE FROM feed_entries WHERE received_at < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

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
