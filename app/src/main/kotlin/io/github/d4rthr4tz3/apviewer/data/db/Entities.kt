package io.github.d4rthr4tz3.apviewer.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A stored feed message of a multiworld. The message itself is kept as JSON. */
@Entity(
    tableName = "feed_entries",
    indices = [Index("multiworld_id"), Index("received_at")],
)
data class FeedEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "multiworld_id") val multiworldId: String,
    @ColumnInfo(name = "received_at") val receivedAtMillis: Long,
    /** Server message type, such as `ItemSend`; kept as a column for filters. */
    val type: String?,
    /** Name of the message's `FeedHighlight`; kept as a column for filters. */
    val highlight: String,
    @ColumnInfo(name = "message_json") val messageJson: String,
)

/** Last reported `ReceivedItems` index of a slot, see `ItemIndexStore`. */
@Entity(tableName = "item_indices", primaryKeys = ["server", "seed_name", "team", "slot"])
data class ItemIndexEntity(
    val server: String,
    @ColumnInfo(name = "seed_name") val seedName: String,
    val team: Int,
    val slot: Int,
    @ColumnInfo(name = "last_index") val lastIndex: Int,
)

/** Cached data package of one game, valid for [checksum]. */
@Entity(tableName = "data_packages")
data class DataPackageEntity(
    @PrimaryKey val game: String,
    val checksum: String,
    @ColumnInfo(name = "data_json") val dataJson: String,
)
