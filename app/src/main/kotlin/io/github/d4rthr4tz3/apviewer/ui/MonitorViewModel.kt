package io.github.d4rthr4tz3.apviewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.d4rthr4tz3.apviewer.protocol.client.OkHttpPacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.monitor.ArchipelagoMonitor
import io.github.d4rthr4tz3.apviewer.protocol.monitor.ConnectionConfig
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.monitor.MonitorState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient

/** Holds the [ArchipelagoMonitor] of the app for the lifetime of the activity. */
class MonitorViewModel : ViewModel() {
    private val monitor = ArchipelagoMonitor(
        scope = viewModelScope,
        socketFactory = OkHttpPacketSocket.Factory(httpClient),
    )

    val state: StateFlow<MonitorState> = monitor.state
    val feed: StateFlow<List<FeedEntry>> = monitor.feed

    fun connect(config: ConnectionConfig) = monitor.connect(config)

    fun disconnect() = monitor.disconnect()

    private companion object {
        const val PING_INTERVAL_SECONDS = 30L

        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .build()
        }
    }
}
