package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.message.MessageRenderer
import io.github.d4rthr4tz3.apviewer.protocol.message.NameResolver
import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import io.github.d4rthr4tz3.apviewer.protocol.model.Hint
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FailureReason
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ConnectionTags
import io.github.d4rthr4tz3.apviewer.protocol.packet.ItemsHandling
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import io.github.d4rthr4tz3.apviewer.protocol.store.DataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.store.SlotKey
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement

/**
 * Connection of one slot as a tracker. It never sends checks. It reconnects with backoff after an
 * established connection breaks, reports received items once per [SlotKey] through
 * [itemIndexStore], and caches item and location names in [dataPackageCache].
 */
class SlotConnection(
    private val scope: CoroutineScope,
    private val socketFactory: PacketSocketFactory,
    val config: SlotConnectionConfig,
    private val itemIndexStore: ItemIndexStore,
    private val dataPackageCache: DataPackageCache,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    private val uuid: String = UUID.randomUUID().toString(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val online: StateFlow<Boolean> = MutableStateFlow(true),
) {
    private val _state = MutableStateFlow<SlotState>(SlotState.Idle)
    private val _messages = MutableSharedFlow<FeedMessage>()
    private val _receivedItems = MutableSharedFlow<ReceivedItem>()
    private val _items = MutableStateFlow<List<ReceivedItem>>(emptyList())
    private val _lastReportedIndex = MutableStateFlow<Int?>(null)
    private val _hints = MutableStateFlow<List<SlotHint>>(emptyList())
    private var job: Job? = null
    private val retry = Channel<Unit>(Channel.CONFLATED)

    @Volatile private var session: Session? = null

    @Volatile private var socket: PacketSocket? = null

    /** Current connection state. */
    val state: StateFlow<SlotState> = _state.asStateFlow()

    /** Rendered `PrintJSON` messages. Empty unless [SlotConnectionConfig.receivesText]. */
    val messages: SharedFlow<FeedMessage> = _messages.asSharedFlow()

    /** Items the slot received, on every connection, with [ReceivedItem.isNew] for new ones. */
    val receivedItems: SharedFlow<ReceivedItem> = _receivedItems.asSharedFlow()

    /**
     * All items the slot received, in index order, as last sent by the server. Kept while
     * reconnecting and cleared by [stop].
     */
    val items: StateFlow<List<ReceivedItem>> = _items.asStateFlow()

    /**
     * Index of the last item that was reported, `-1` if none, or `null` before the first
     * connection. Items up to this index never count as new again.
     */
    val lastReportedIndex: StateFlow<Int?> = _lastReportedIndex.asStateFlow()

    /**
     * Hints for and from this slot, as last read from the server. Kept while reconnecting and
     * cleared by [stop].
     */
    val hints: StateFlow<List<SlotHint>> = _hints.asStateFlow()

    /** Connects, or reconnects if already started. */
    fun start() {
        job?.cancel()
        // Set synchronously so that observers never see the previous state after this call.
        _state.value = SlotState.Connecting(candidateUrls(config.address).first())
        job = scope.launch { run() }
    }

    /** Closes the connection and stops reconnecting. */
    fun stop() {
        job?.cancel()
        job = null
        socket = null
        _state.value = SlotState.Idle
        _hints.value = emptyList()
        _items.value = emptyList()
    }

    /**
     * Treats every item received so far as reported, so only items received from now on are new.
     * Returns `false` if not connected.
     */
    fun countFromNow(): Boolean {
        val current = session?.takeIf { _state.value is SlotState.Connected } ?: return false
        scope.launch { current.markAllReported() }
        return true
    }

    /**
     * Ends the wait before the next reconnect attempt, for example when the network is back.
     * Returns `false` if the slot is not waiting to reconnect.
     */
    fun retryNow(): Boolean {
        if (_state.value !is SlotState.Reconnecting) return false
        retry.trySend(Unit)
        return true
    }

    /** Sends [text] as chat message or command. Returns `false` if not connected. */
    fun say(text: String): Boolean {
        if (_state.value !is SlotState.Connected) return false
        return socket?.send(listOf(ClientPacket.Say(text))) ?: false
    }

    private suspend fun run() {
        var attempt = 0
        var everConnected = false
        var preferredUrl: String? = null
        while (true) {
            if (!online.value) {
                // Attempts without a network only fail; wait until it is back instead.
                _state.value = SlotState.Offline
                online.first { it }
                attempt = 0
            }
            val outcome = connectOnce(preferredUrl)
            everConnected = everConnected || outcome.connected
            preferredUrl = outcome.url ?: preferredUrl
            // Without a network the server only looks unreachable, so the next round waits for it.
            val gaveUp = !everConnected && online.value
            if (outcome.reason is FailureReason.Refused || gaveUp) {
                _state.value = SlotState.Failed(outcome.reason)
                return
            }
            attempt = if (outcome.connected) 1 else attempt + 1
            val delayMillis = reconnectPolicy.delayMillis(attempt)
            // A request from before this wait must not cut it short.
            retry.tryReceive()
            _state.value = SlotState.Reconnecting(
                attempt = attempt,
                delayMillis = delayMillis,
                reason = outcome.reason,
                lastUrl = preferredUrl ?: candidateUrls(config.address).first(),
            )
            withTimeoutOrNull(delayMillis) { retry.receive() }
        }
    }

    private class Outcome(val reason: FailureReason, val connected: Boolean, val url: String?)

    private suspend fun connectOnce(preferredUrl: String?): Outcome {
        val urls = candidateUrls(config.address).let { urls ->
            if (preferredUrl == null) urls else listOf(preferredUrl) + (urls - preferredUrl)
        }
        var lastError: Exception? = null
        for (url in urls) {
            _state.value = SlotState.Connecting(url)
            val opened = try {
                socketFactory.open(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                continue
            }
            socket = opened
            val current = Session(url, opened)
            session = current
            try {
                val reason = current.run()
                return Outcome(reason, current.connected, url)
            } finally {
                session = null
                socket = null
                opened.close()
            }
        }
        return Outcome(FailureReason.Unreachable(lastError?.message), connected = false, url = null)
    }

    private inner class Session(private val url: String, private val socket: PacketSocket) {
        private var names = NameResolver()
        private var renderer = MessageRenderer(ownSlots = emptySet())
        private var serverVersion = ""
        private var seedName = ""
        private var team = 0
        private var ownSlot = -1
        private var slotKey: SlotKey? = null
        private var lastReportedIndex = -1
        private var nextIndex = 0
        private var refusal: FailureReason.Refused? = null
        private var hintsKey: String? = null

        var connected = false
            private set

        /** Runs until the connection ends and returns why it ended. */
        suspend fun run(): FailureReason = try {
            socket.incoming.collect { frame -> handleFrame(frame) }
            refusal ?: FailureReason.ConnectionLost(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            refusal ?: FailureReason.ConnectionLost(e.message)
        }

        private suspend fun handleFrame(frame: List<ServerPacket>) {
            for (packet in frame) {
                when (packet) {
                    is ServerPacket.RoomInfo -> onRoomInfo(packet)

                    is ServerPacket.DataPackage -> onDataPackage(packet)

                    is ServerPacket.Connected -> onConnected(packet, frame)

                    is ServerPacket.ConnectionRefused -> {
                        refusal = FailureReason.Refused(packet.errors)
                        socket.close()
                    }

                    is ServerPacket.ReceivedItems -> onReceivedItems(packet)

                    is ServerPacket.RoomUpdate -> packet.players?.let { players ->
                        names = names.withPlayers(team, players)
                    }

                    is ServerPacket.PrintJson -> {
                        if (config.receivesText) {
                            _messages.emit(renderer.render(packet, names))
                        }
                    }

                    is ServerPacket.Retrieved -> hintsKey?.let { key ->
                        if (key in packet.keys) onHints(packet.keys[key])
                    }

                    is ServerPacket.SetReply -> if (packet.key == hintsKey) onHints(packet.value)

                    is ServerPacket.Unknown -> Unit
                }
            }
        }

        private suspend fun onRoomInfo(packet: ServerPacket.RoomInfo) {
            serverVersion = packet.version.toString()
            seedName = packet.seedName
            val cached = mutableMapOf<String, GameData>()
            val missing = mutableListOf<String>()
            for (game in packet.games) {
                val checksum = packet.dataPackageChecksums[game].orEmpty()
                val data = dataPackageCache.get(game, checksum)
                if (data != null) cached[game] = data else missing += game
            }
            names = names.withGames(cached)
            val packets = buildList {
                if (missing.isNotEmpty()) add(ClientPacket.GetDataPackage(games = missing))
                add(
                    ClientPacket.Connect(
                        name = config.slotName,
                        password = config.password,
                        uuid = uuid,
                        // Reporting the server's own version passes even a room that demands an
                        // exact version match; trackers have no game that needs a newer client.
                        version = packet.version,
                        itemsHandling = ItemsHandling.ALL,
                        tags = buildList {
                            add(ConnectionTags.TRACKER)
                            if (!config.receivesText) add(ConnectionTags.NO_TEXT)
                        },
                    ),
                )
            }
            socket.send(packets)
        }

        private suspend fun onDataPackage(packet: ServerPacket.DataPackage) {
            names = names.withGames(packet.data.games)
            packet.data.games.forEach { (game, data) -> dataPackageCache.put(game, data) }
        }

        private suspend fun onConnected(packet: ServerPacket.Connected, frame: List<ServerPacket>) {
            team = packet.team
            ownSlot = packet.slot
            names = names.withPlayers(packet.team, packet.players, packet.slotInfo)
            val ownSlots = packet.players
                .filter { it.team == packet.team && it.name in config.ownSlotNames }
                .map { it.slot }
                .toSet() +
                listOfNotNull(packet.slot.takeIf { config.slotName in config.ownSlotNames })
            renderer = MessageRenderer(ownSlots)
            val indexKey = SlotKey(serverKey(config.address), seedName, packet.team, packet.slot)
            slotKey = indexKey
            val stored = itemIndexStore.lastReportedIndex(indexKey)
            lastReportedIndex = stored ?: baselineIndex(frame)
            if (stored == null) {
                itemIndexStore.setLastReportedIndex(indexKey, lastReportedIndex)
            }
            _lastReportedIndex.value = lastReportedIndex
            nextIndex = 0
            connected = true
            val key = Hint.storageKey(packet.team, packet.slot)
            hintsKey = key
            socket.send(listOf(ClientPacket.Get(listOf(key)), ClientPacket.SetNotify(listOf(key))))
            _state.value = SlotState.Connected(
                SlotInfo(
                    url = url,
                    seedName = seedName,
                    serverVersion = serverVersion,
                    team = packet.team,
                    slot = packet.slot,
                    playerName = names.playerName(packet.slot),
                    game = packet.slotInfo[packet.slot]?.game,
                    players = packet.players
                        .filter { it.team == packet.team }
                        .map { RoomPlayer(it.slot, it.name, packet.slotInfo[it.slot]?.game) },
                ),
            )
        }

        private fun onHints(value: JsonElement?) {
            _hints.value = Hint.listFrom(value).map { hint ->
                SlotHint(
                    receivingSlot = hint.receivingPlayer,
                    receiverName = names.playerName(hint.receivingPlayer),
                    findingSlot = hint.findingPlayer,
                    finderName = names.playerName(hint.findingPlayer),
                    itemId = hint.item,
                    itemName = names.itemName(hint.item, hint.receivingPlayer),
                    itemFlags = hint.itemFlags,
                    locationId = hint.location,
                    locationName = names.locationName(hint.location, hint.findingPlayer),
                    entrance = hint.entrance,
                    status = hint.hintStatus,
                )
            }
        }

        /**
         * Returns the index to start reporting after when the slot is seen for the first time: the
         * items the server sends right with `Connected` are history, everything later is new.
         */
        private fun baselineIndex(frame: List<ServerPacket>): Int {
            val initial = frame.filterIsInstance<ServerPacket.ReceivedItems>()
                .firstOrNull { it.index == 0 }
            return (initial?.items?.size ?: 0) - 1
        }

        private suspend fun onReceivedItems(packet: ServerPacket.ReceivedItems) {
            if (packet.index != nextIndex && packet.index != 0) {
                // Missed items: ask for the full list, which arrives with index 0.
                socket.send(listOf(ClientPacket.Sync))
                return
            }
            val now = clock()
            val known = _items.value
            val received = packet.items.mapIndexed { offset, item ->
                val index = packet.index + offset
                val isNew = index > lastReportedIndex
                ReceivedItem(
                    index = index,
                    item = item,
                    itemName = names.itemName(item.item, ownSlot),
                    senderName = names.playerName(item.player),
                    locationName = names.locationName(item.location, item.player),
                    isNew = isNew,
                    receivedAtMillis = if (isNew) now else known.getOrNull(index)?.receivedAtMillis,
                )
            }
            _items.value = if (packet.index == 0) received else _items.value + received
            received.forEach { _receivedItems.emit(it) }
            nextIndex = packet.index + packet.items.size
            if (nextIndex - 1 > lastReportedIndex) {
                markReported(nextIndex - 1)
            }
        }

        suspend fun markAllReported() {
            markReported(maxOf(nextIndex - 1, lastReportedIndex))
        }

        private suspend fun markReported(index: Int) {
            val key = slotKey ?: return
            lastReportedIndex = index
            itemIndexStore.setLastReportedIndex(key, index)
            _lastReportedIndex.value = index
        }
    }

    companion object {
        /** Default port of Archipelago servers. */
        const val DEFAULT_PORT = 38281

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

        /** Returns `host:port` of [address], used to tell servers apart in [SlotKey]. */
        fun serverKey(address: String): String =
            candidateUrls(address).first().substringAfter("://").lowercase()
    }
}
