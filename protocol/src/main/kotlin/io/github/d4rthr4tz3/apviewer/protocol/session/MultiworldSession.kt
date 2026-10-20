package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.store.DataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemIndexStore
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One of the user's slots in a multiworld. */
data class SlotConfig(
    val name: String,
    /** Whether items for and from this slot are highlighted in the feed. */
    val highlight: Boolean = true,
)

/** A multiworld room and the user's slots in it. */
data class MultiworldConfig(
    val address: String,
    /** Server password; Archipelago has one password per room, not per slot. */
    val roomPassword: String = "",
    val slots: List<SlotConfig>,
)

/** An item received by one of the slots of a [MultiworldSession]. */
data class SlotReceivedItem(val slotName: String, val item: ReceivedItem)

/**
 * Connections of all of the user's slots in one multiworld. Every slot receives the room's text.
 * The first connected slot provides the messages sent to the whole room; the others only add the
 * messages the server sends to them alone, such as their hints and command answers. Together they
 * form [feed].
 */
class MultiworldSession(
    private val scope: CoroutineScope,
    socketFactory: PacketSocketFactory,
    val config: MultiworldConfig,
    itemIndexStore: ItemIndexStore,
    dataPackageCache: DataPackageCache,
    reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    uuid: String = UUID.randomUUID().toString(),
    private val maxFeedSize: Int = DEFAULT_MAX_FEED_SIZE,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _feed = MutableStateFlow<List<FeedEntry>>(emptyList())
    private val _receivedItems = MutableSharedFlow<SlotReceivedItem>()
    private val collectors = mutableListOf<Job>()
    private var nextEntryId = 0L

    init {
        require(config.slots.isNotEmpty()) { "A multiworld needs at least one slot" }
    }

    /** One connection per slot, in the order of [MultiworldConfig.slots]. */
    val slots: List<SlotConnection> = config.slots.mapIndexed { index, slot ->
        SlotConnection(
            scope = scope,
            socketFactory = socketFactory,
            config = SlotConnectionConfig(
                address = config.address,
                slotName = slot.name,
                password = config.roomPassword,
                ownSlotNames = config.slots.filter { it.highlight }.map { it.name }.toSet(),
            ),
            itemIndexStore = itemIndexStore,
            dataPackageCache = dataPackageCache,
            reconnectPolicy = reconnectPolicy,
            uuid = uuid,
        )
    }

    /** Messages of the room, oldest first, limited to the newest `maxFeedSize`. */
    val feed: StateFlow<List<FeedEntry>> = _feed.asStateFlow()

    /** Items received by any of the slots. */
    val receivedItems: SharedFlow<SlotReceivedItem> = _receivedItems.asSharedFlow()

    /**
     * Hints of all connected slots, each hint once. A hint between two of the user's slots is known
     * to both of them.
     */
    val hints: Flow<List<MultiworldHint>> =
        combine(slots.map { slot -> combine(slot.state, slot.hints, ::Pair) }) { perSlot ->
            val ownSlots = perSlot.mapNotNull { (state, _) ->
                (state as? SlotState.Connected)?.info?.slot
            }.toSet()
            perSlot.flatMap { (_, hints) -> hints }
                .distinctBy { it.findingSlot to it.locationId }
                .map { hint ->
                    MultiworldHint(
                        hint = hint,
                        receiverIsOwn = hint.receivingSlot in ownSlots,
                        finderIsOwn = hint.findingSlot in ownSlots,
                    )
                }
        }

    /**
     * Connects all slots. The feed is kept across reconnects and cleared only here. Collectors start
     * undispatched so that nothing a slot emits right after connecting is missed.
     */
    fun start() {
        stopCollectors()
        _feed.value = emptyList()
        slots.forEachIndexed { index, slot ->
            collectors += scope.launch(start = CoroutineStart.UNDISPATCHED) {
                slot.messages.collect { message ->
                    if (takesMessage(index, message)) append(message)
                }
            }
        }
        slots.forEach { slot ->
            collectors += scope.launch(start = CoroutineStart.UNDISPATCHED) {
                slot.receivedItems.collect { item ->
                    _receivedItems.emit(SlotReceivedItem(slot.config.slotName, item))
                }
            }
        }
        slots.forEach(SlotConnection::start)
    }

    /** Disconnects all slots. */
    fun stop() {
        slots.forEach(SlotConnection::stop)
        stopCollectors()
    }

    /** Sends [text] as chat message or command from [slotName]. Returns `false` if not sent. */
    fun say(slotName: String, text: String): Boolean =
        slots.firstOrNull { it.config.slotName == slotName }?.say(text) ?: false

    @Synchronized
    private fun append(message: FeedMessage) {
        val entry = FeedEntry(id = nextEntryId++, message = message, receivedAtMillis = clock())
        _feed.update { feed -> (feed + entry).takeLast(maxFeedSize) }
    }

    /**
     * Returns whether the message the slot at [index] received belongs in the feed. The server
     * sends room-wide messages to every slot, so only the first connected slot keeps them. Command
     * answers go to the sending slot alone, and a hint goes to its receiving and finding slots, of
     * which the first connected one keeps it.
     */
    private fun takesMessage(index: Int, message: FeedMessage): Boolean {
        val connected = slots.map { (it.state.value as? SlotState.Connected)?.info?.slot }
        return when (message.type) {
            in DIRECT_MESSAGE_TYPES -> true

            "Hint" -> {
                val involved = setOfNotNull(message.receivingSlot, message.item?.player)
                connected.indexOfFirst { it in involved }.let { it == index || it < 0 }
            }

            else -> connected.indexOfFirst { it != null }.let { it == index || it < 0 }
        }
    }

    private fun stopCollectors() {
        collectors.forEach(Job::cancel)
        collectors.clear()
    }

    private companion object {
        const val DEFAULT_MAX_FEED_SIZE = 500

        /** `PrintJSON` types the server sends only to the connection that caused them. */
        val DIRECT_MESSAGE_TYPES = setOf("CommandResult", "AdminCommandResult")
    }
}
