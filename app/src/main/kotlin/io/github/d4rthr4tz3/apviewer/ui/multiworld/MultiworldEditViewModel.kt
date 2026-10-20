package io.github.d4rthr4tz3.apviewer.ui.multiworld

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.d4rthr4tz3.apviewer.AppContainer
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import io.github.d4rthr4tz3.apviewer.protocol.session.ProbeResult
import io.github.d4rthr4tz3.apviewer.protocol.session.RoomPlayer
import io.github.d4rthr4tz3.apviewer.protocol.session.ServerAddress
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotConnection
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotState
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedMultiworld
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedSlot
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The editable settings of a multiworld, saved only with [MultiworldEditViewModel.save]. */
data class MultiworldDraft(
    val name: String = "",
    val host: String = DEFAULT_HOST,
    val port: String = SlotConnection.DEFAULT_PORT.toString(),
    val roomPassword: String = "",
    val slots: List<SavedSlot> = emptyList(),
    val connectOnStart: Boolean = true,
    val keepInBackground: Boolean = true,
    val feedRetentionDays: Int = SavedMultiworld.DEFAULT_FEED_RETENTION_DAYS,
) {
    val address: String get() = ServerAddress(host, port).toString()

    val canSave: Boolean get() = host.isNotBlank()

    companion object {
        /** Host of the public Archipelago server, where most rooms run. */
        const val DEFAULT_HOST = "archipelago.gg"
    }
}

sealed interface ProbeState {
    data object Idle : ProbeState

    data object Checking : ProbeState

    data class Done(val result: ProbeResult) : ProbeState
}

/** Live state of a slot of the running session. */
data class LiveSlot(val state: SlotState, val lastReportedIndex: Int?)

/**
 * Edits the multiworld with [multiworldId], or a new one. The slot settings screen edits slots of
 * the same draft.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiworldEditViewModel(private val container: AppContainer, multiworldId: String?) :
    ViewModel() {
    val id: String = multiworldId ?: UUID.randomUUID().toString()
    val isNew: Boolean = multiworldId == null

    private val _draft = MutableStateFlow<MultiworldDraft?>(null)
    private val _probe = MutableStateFlow<ProbeState>(ProbeState.Idle)
    private var probeJob: Job? = null
    private var slotSaveJob: Job? = null

    /**
     * Stored name of each slot of the draft, by index; `null` for slots added to the draft and not
     * stored yet. Lets [editSlot] store a renamed slot in place of the old one.
     */
    private val storedSlotNames = mutableListOf<String?>()

    /** The draft, `null` while loading. */
    val draft: StateFlow<MultiworldDraft?> = _draft.asStateFlow()

    val probe: StateFlow<ProbeState> = _probe.asStateFlow()

    val status: StateFlow<MultiworldStatus> = container.sessions.status(id)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            MultiworldStatus.Disconnected,
        )

    /** Slots of the room, known once one of the user's slots is connected. */
    val roomPlayers: StateFlow<List<RoomPlayer>> = container.sessions.sessions
        .map { it[id] }
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                combine(session.slots.map { it.state }) { states ->
                    states.firstNotNullOfOrNull {
                        (it as? SlotState.Connected)?.info?.players
                    }.orEmpty()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    init {
        viewModelScope.launch {
            val saved = multiworldId?.let { container.profiles.profiles.first().multiworld(it) }
            _draft.value = saved?.toDraft() ?: MultiworldDraft()
            storedSlotNames += saved?.slots?.map { it.name }.orEmpty()
            if (saved != null) runProbe()
        }
    }

    /** Live state of the slot named [slotName] while the multiworld is connected. */
    fun liveSlot(slotName: String): Flow<LiveSlot?> = container.sessions.sessions
        .map { sessions -> sessions[id]?.slots?.firstOrNull { it.config.slotName == slotName } }
        .flatMapLatest { slot ->
            if (slot == null) {
                flowOf(null)
            } else {
                combine(slot.state, slot.lastReportedIndex) { state, index ->
                    LiveSlot(state, index)
                }
            }
        }

    fun edit(transform: (MultiworldDraft) -> MultiworldDraft) {
        _draft.update { it?.let(transform) }
    }

    /**
     * Changes the slot at [index]. A slot of a stored multiworld is stored right away, without the
     * rest of the draft; typing is awaited so that a rename does not reconnect on every key.
     */
    fun editSlot(index: Int, transform: (SavedSlot) -> SavedSlot) {
        edit { draft ->
            val slots = draft.slots.toMutableList()
            slots[index] = transform(slots[index])
            draft.copy(slots = slots)
        }
        val storedName = storedSlotNames.getOrNull(index) ?: return
        slotSaveJob?.cancel()
        slotSaveJob = viewModelScope.launch {
            delay(SLOT_SAVE_DELAY_MILLIS)
            val slot = _draft.value?.slots?.getOrNull(index) ?: return@launch
            if (slot.name.isBlank()) return@launch
            storeSlots { slots -> slots.map { if (it.name == storedName) slot else it } }
            storedSlotNames[index] = slot.name
        }
    }

    /** Adds a slot named [name] unless the draft has one with that name already. */
    fun addSlot(name: String) = edit { draft ->
        val trimmed = name.trim()
        if (trimmed.isEmpty() || draft.slots.any { it.name == trimmed }) {
            draft
        } else {
            storedSlotNames += null
            draft.copy(slots = draft.slots + SavedSlot(trimmed))
        }
    }

    /** Removes the slot at [index]; a stored slot is removed from the stored multiworld at once. */
    fun removeSlot(index: Int) {
        edit { draft -> draft.copy(slots = draft.slots.filterIndexed { i, _ -> i != index }) }
        val storedName = storedSlotNames.removeAt(index) ?: return
        slotSaveJob?.cancel()
        viewModelScope.launch { storeSlots { slots -> slots.filterNot { it.name == storedName } } }
    }

    /** Changes the slots of the stored multiworld only, in the app scope like [save]. */
    private suspend fun storeSlots(transform: (List<SavedSlot>) -> List<SavedSlot>) {
        container.scope.launch {
            container.profiles.update { profiles ->
                val saved = profiles.multiworld(id) ?: return@update profiles
                val slots = transform(saved.slots).withSeenIndices(saved)
                profiles.withMultiworld(saved.copy(slots = slots))
            }
        }.join()
    }

    /** Checks whether the server of the draft answers. */
    fun runProbe() {
        val address = _draft.value?.takeIf { it.canSave }?.address ?: return
        probeJob?.cancel()
        _probe.value = ProbeState.Checking
        probeJob = viewModelScope.launch {
            _probe.value = ProbeState.Done(container.probe.probe(address))
        }
    }

    /**
     * Saves the draft. Runs in the app scope, so it completes even though the screen closes right
     * after. Returns `false` if the draft cannot be saved yet.
     */
    fun save(): Boolean {
        val draft = _draft.value?.takeIf { it.canSave } ?: return false
        container.scope.launch {
            container.profiles.update { profiles ->
                val saved = profiles.multiworld(id)
                profiles.withMultiworld(
                    SavedMultiworld(
                        id = id,
                        label = draft.name.trim(),
                        address = draft.address,
                        roomPassword = draft.roomPassword,
                        slots = saved?.let { draft.slots.withSeenIndices(it) } ?: draft.slots,
                        connectOnStart = draft.connectOnStart,
                        keepInBackground = draft.keepInBackground,
                        feedRetentionDays = draft.feedRetentionDays,
                        lastSeenFeedId = saved?.lastSeenFeedId ?: 0,
                    ),
                )
            }
            if (isNew) container.sessions.connect(id)
        }
        return true
    }

    /** Deletes the multiworld and its stored feed. */
    fun remove() {
        container.scope.launch {
            container.profiles.update { it.withoutMultiworld(id) }
            container.feed.deleteMultiworld(id)
        }
    }

    fun connect() = container.sessions.connect(id)

    fun disconnect() = container.sessions.disconnect(id)

    /** See [SlotConnection.countFromNow]. */
    fun countFromNow(slotName: String) {
        container.sessions.sessions.value[id]?.slots
            ?.firstOrNull { it.config.slotName == slotName }
            ?.countFromNow()
    }

    private fun SavedMultiworld.toDraft(): MultiworldDraft {
        val address = ServerAddress.parse(address)
        return MultiworldDraft(
            name = label,
            host = address.host,
            port = address.port,
            roomPassword = roomPassword,
            slots = slots,
            connectOnStart = connectOnStart,
            keepInBackground = keepInBackground,
            feedRetentionDays = feedRetentionDays,
        )
    }

    /**
     * Keeps the stored index of the last item seen in the Items tab, which changes while the draft
     * is open.
     */
    private fun List<SavedSlot>.withSeenIndices(saved: SavedMultiworld): List<SavedSlot> =
        map { slot ->
            saved.slots.firstOrNull { it.name == slot.name }
                ?.let { slot.copy(lastSeenItemIndex = it.lastSeenItemIndex) }
                ?: slot
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val SLOT_SAVE_DELAY_MILLIS = 600L
    }
}
