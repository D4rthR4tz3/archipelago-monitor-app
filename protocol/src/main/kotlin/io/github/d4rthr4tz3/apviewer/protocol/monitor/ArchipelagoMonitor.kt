package io.github.d4rthr4tz3.apviewer.protocol.monitor

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageRenderer
import io.github.d4rthr4tz3.apviewer.protocol.message.NameResolver
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkVersion
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ConnectionTags
import io.github.d4rthr4tz3.apviewer.protocol.packet.ItemsHandling
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Watches one slot of an Archipelago room as a tracker: it never sends checks, it only collects
 * the room's messages into [feed].
 */
class ArchipelagoMonitor(
    private val scope: CoroutineScope,
    private val socketFactory: PacketSocketFactory,
    private val clientVersion: NetworkVersion = DEFAULT_CLIENT_VERSION,
    private val uuid: String = UUID.randomUUID().toString(),
    private val maxFeedSize: Int = DEFAULT_MAX_FEED_SIZE,
) {
    private val _state = MutableStateFlow<MonitorState>(MonitorState.Disconnected)
    private val _feed = MutableStateFlow<List<FeedEntry>>(emptyList())
    private var job: Job? = null

    /** Current connection state. */
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    /** Messages of the current connection, oldest first, limited to the newest `maxFeedSize`. */
    val feed: StateFlow<List<FeedEntry>> = _feed.asStateFlow()

    /** Closes any open connection and connects to the slot described by [config]. */
    fun connect(config: ConnectionConfig) {
        job?.cancel()
        _feed.value = emptyList()
        job = scope.launch { run(config) }
    }

    /** Closes the connection. */
    fun disconnect() {
        job?.cancel()
        job = null
        _state.value = MonitorState.Disconnected
    }

    private suspend fun run(config: ConnectionConfig) {
        var lastError: Exception? = null
        for (url in candidateUrls(config.address)) {
            _state.value = MonitorState.Connecting(url)
            val socket = try {
                socketFactory.open(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                continue
            }
            try {
                Session(url, socket, config).run()
            } finally {
                socket.close()
            }
            return
        }
        _state.value = MonitorState.Failed(FailureReason.Unreachable(lastError?.message))
    }

    private inner class Session(
        private val url: String,
        private val socket: PacketSocket,
        private val config: ConnectionConfig,
    ) {
        private var names = NameResolver()
        private var renderer = MessageRenderer(ownSlot = null)
        private var serverVersion = ""
        private var seedName = ""
        private var team = 0
        private var nextEntryId = 0L

        suspend fun run() {
            try {
                socket.incoming.collect(::handle)
                if (_state.value !is MonitorState.Failed) {
                    _state.value = MonitorState.Failed(FailureReason.ConnectionLost(null))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = MonitorState.Failed(FailureReason.ConnectionLost(e.message))
            }
        }

        private fun handle(packet: ServerPacket) {
            when (packet) {
                is ServerPacket.RoomInfo -> onRoomInfo(packet)

                is ServerPacket.DataPackage -> names = names.withGames(packet.data.games)

                is ServerPacket.Connected -> onConnected(packet)

                is ServerPacket.ConnectionRefused -> {
                    _state.value = MonitorState.Failed(FailureReason.Refused(packet.errors))
                    socket.close()
                }

                is ServerPacket.RoomUpdate -> packet.players?.let { players ->
                    names = names.withPlayers(team, players)
                }

                is ServerPacket.PrintJson -> append(packet)

                is ServerPacket.ReceivedItems, is ServerPacket.Unknown -> Unit
            }
        }

        private fun onRoomInfo(packet: ServerPacket.RoomInfo) {
            serverVersion = packet.version.toString()
            seedName = packet.seedName
            socket.send(
                listOf(
                    ClientPacket.GetDataPackage(games = packet.games),
                    ClientPacket.Connect(
                        name = config.slotName,
                        password = config.password,
                        uuid = uuid,
                        version = clientVersion,
                        itemsHandling = ItemsHandling.ALL,
                        tags = listOf(ConnectionTags.TRACKER),
                    ),
                ),
            )
        }

        private fun onConnected(packet: ServerPacket.Connected) {
            team = packet.team
            names = names.withPlayers(packet.team, packet.players, packet.slotInfo)
            renderer = MessageRenderer(ownSlot = packet.slot)
            _state.value = MonitorState.Connected(
                url = url,
                seedName = seedName,
                serverVersion = serverVersion,
                team = packet.team,
                slot = packet.slot,
                playerName = names.playerName(packet.slot),
            )
        }

        private fun append(packet: ServerPacket.PrintJson) {
            val entry = FeedEntry(id = nextEntryId++, message = renderer.render(packet, names))
            _feed.update { feed -> (feed + entry).takeLast(maxFeedSize) }
        }
    }

    companion object {
        /** Default port of Archipelago servers. */
        const val DEFAULT_PORT = 38281

        /** Version reported to the server; trackers without a game skip version checks. */
        val DEFAULT_CLIENT_VERSION = NetworkVersion(major = 0, minor = 6, build = 0)

        private const val DEFAULT_MAX_FEED_SIZE = 500

        /**
         * Returns the WebSocket URLs to try for [address]: the address itself if it has a scheme,
         * otherwise `wss://` first and `ws://` as fallback, with [DEFAULT_PORT] if none is given.
         */
        fun candidateUrls(address: String): List<String> {
            val trimmed = address.trim().removeSuffix("/")
            if (trimmed.startsWith("ws://") || trimmed.startsWith("wss://")) {
                return listOf(trimmed)
            }
            val hasPort = ':' in trimmed.substringAfterLast(']')
            val hostAndPort = if (hasPort) trimmed else "$trimmed:$DEFAULT_PORT"
            return listOf("wss://$hostAndPort", "ws://$hostAndPort")
        }
    }
}
