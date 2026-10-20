package io.github.d4rthr4tz3.apviewer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** The app's database: feed history, item indices and the data package cache. */
@Database(
    entities = [FeedEntryEntity::class, ItemIndexEntity::class, DataPackageEntity::class],
    version = 1,
)
abstract class ApDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao

    abstract fun itemIndexDao(): ItemIndexDao

    abstract fun dataPackageDao(): DataPackageDao

    companion object {
        private const val NAME = "apviewer.db"

        fun create(context: Context): ApDatabase =
            Room.databaseBuilder(context, ApDatabase::class.java, NAME).build()
    }
}
