package io.github.d4rthr4tz3.apviewer

import android.content.Context
import io.github.d4rthr4tz3.apviewer.data.FeedRepository
import io.github.d4rthr4tz3.apviewer.data.ProfilesRepository
import io.github.d4rthr4tz3.apviewer.data.RoomDataPackageCache
import io.github.d4rthr4tz3.apviewer.data.RoomItemIndexStore
import io.github.d4rthr4tz3.apviewer.data.db.ApDatabase
import io.github.d4rthr4tz3.apviewer.protocol.client.OkHttpPacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.store.DataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemIndexStore
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/** App-wide singletons, created once per process by [ApViewerApplication]. */
class AppContainer(context: Context) {
    private val database = ApDatabase.create(context)

    val profiles: ProfilesRepository = ProfilesRepository.create(context)
    val feed: FeedRepository = FeedRepository(database.feedDao())
    val itemIndexStore: ItemIndexStore = RoomItemIndexStore(database.itemIndexDao())
    val dataPackageCache: DataPackageCache = RoomDataPackageCache(database.dataPackageDao())

    val socketFactory: PacketSocketFactory = OkHttpPacketSocket.Factory(
        OkHttpClient.Builder()
            .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .build(),
    )

    private companion object {
        const val PING_INTERVAL_SECONDS = 30L
    }
}
