package io.github.d4rthr4tz3.apviewer.protocol.monitor

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldConfig
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldSession
import io.github.d4rthr4tz3.apviewer.protocol.session.ReconnectPolicy
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotConfig
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotConnection
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotState
import io.github.d4rthr4tz3.apviewer.protocol.store.DataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryDataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.InMemoryItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemIndexStore
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Watches one slot of an Archipelago room, for the single-slot screens of the app. It is a thin
 * layer over a [MultiworldSession] with one slot and maps its state to [MonitorState]; while the
 * session reconnects, the state is [MonitorState.Connecting].
 */
class ArchipelagoMonitor(
    private val scope: CoroutineScope,
    private val socketFactory: PacketSocketFactory,
    private val itemIndexStore: ItemIndexStore = InMemoryItemIndexStore(),
    private val dataPackageCache: DataPackageCache = InMemoryDataPackageCache(),
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    private val uuid: String = UUID.randomUUID().toString(),
) {
    private val _state = MutableStateFlow<MonitorState>(MonitorState.Disconnected)
    private val _feed = MutableStateFlow<List<FeedEntry>>(emptyList())
    private var session: MultiworldSession? = null
    private var observer: Job? = null

    /** Current connection state. */
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    /** Messages of the current connection, oldest first. */
    val feed: StateFlow<List<FeedEntry>> = _feed.asStateFlow()

    /** Closes any open connection and connects to the slot described by [config]. */
    fun connect(config: ConnectionConfig) {
        stopSession()
        val newSession = MultiworldSession(
            scope = scope,
            socketFactory = socketFactory,
            config = MultiworldConfig(
                address = config.address,
                roomPassword = config.password,
                slots = listOf(SlotConfig(name = config.slotName)),
            ),
            itemIndexStore = itemIndexStore,
            dataPackageCache = dataPackageCache,
            reconnectPolicy = reconnectPolicy,
            uuid = uuid,
        )
        session = newSession
        newSession.start()
        val slot = newSession.slots.single()
        // Mapped synchronously so that observers never see the previous state after this call.
        _state.value = slot.state.value.toMonitorState()
        observer = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            launch { slot.state.collect { _state.value = it.toMonitorState() } }
            launch { newSession.feed.collect { _feed.value = it } }
        }
    }

    /** Closes the connection. */
    fun disconnect() {
        stopSession()
        _state.value = MonitorState.Disconnected
    }

    private fun stopSession() {
        observer?.cancel()
        observer = null
        session?.stop()
        session = null
    }

    private fun SlotState.toMonitorState(): MonitorState = when (this) {
        SlotState.Idle -> MonitorState.Disconnected

        is SlotState.Connecting -> MonitorState.Connecting(url)

        is SlotState.Reconnecting -> MonitorState.Connecting(lastUrl)

        is SlotState.Failed -> MonitorState.Failed(reason)

        is SlotState.Connected -> MonitorState.Connected(
            url = info.url,
            seedName = info.seedName,
            serverVersion = info.serverVersion,
            team = info.team,
            slot = info.slot,
            playerName = info.playerName,
        )
    }

    companion object {
        /** Default port of Archipelago servers. */
        const val DEFAULT_PORT = SlotConnection.DEFAULT_PORT

        /** See [SlotConnection.candidateUrls]. */
        fun candidateUrls(address: String): List<String> = SlotConnection.candidateUrls(address)
    }
}
