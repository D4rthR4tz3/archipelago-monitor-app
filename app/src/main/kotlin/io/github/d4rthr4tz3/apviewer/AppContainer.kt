package io.github.d4rthr4tz3.apviewer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import io.github.d4rthr4tz3.apviewer.data.FeedRepository
import io.github.d4rthr4tz3.apviewer.data.ProfilesRepository
import io.github.d4rthr4tz3.apviewer.data.RoomDataPackageCache
import io.github.d4rthr4tz3.apviewer.data.RoomItemIndexStore
import io.github.d4rthr4tz3.apviewer.data.db.ApDatabase
import io.github.d4rthr4tz3.apviewer.protocol.client.OkHttpPacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.session.RoomProbe
import io.github.d4rthr4tz3.apviewer.protocol.session.SessionManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * App-wide singletons, created once per process by [ApViewerApplication]. Connections live in
 * [scope], so they survive configuration changes and leaving a screen.
 */
class AppContainer(context: Context) {
    private val database = ApDatabase.create(context)

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val profiles: ProfilesRepository = ProfilesRepository.create(context)
    val feed: FeedRepository = FeedRepository(database.feedDao())

    private val socketFactory: PacketSocketFactory = OkHttpPacketSocket.Factory(
        OkHttpClient.Builder()
            .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .build(),
    )

    val sessions = SessionManager(
        scope = scope,
        socketFactory = socketFactory,
        itemIndexStore = RoomItemIndexStore(database.itemIndexDao()),
        dataPackageCache = RoomDataPackageCache(database.dataPackageDao()),
        feedSink = feed,
    )

    val probe = RoomProbe(socketFactory)

    init {
        scope.launch {
            feed.prune(profiles.profiles.first().multiworlds)
            profiles.profiles.collect { sessions.update(it) }
        }
        // Waiting out the backoff after the network is back would delay messages for no reason.
        context.getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = sessions.retryNow()
            },
        )
    }

    private companion object {
        const val PING_INTERVAL_SECONDS = 30L
    }
}
