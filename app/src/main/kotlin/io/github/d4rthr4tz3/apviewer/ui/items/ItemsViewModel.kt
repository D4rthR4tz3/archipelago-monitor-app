package io.github.d4rthr4tz3.apviewer.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.d4rthr4tz3.apviewer.AppContainer
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import io.github.d4rthr4tz3.apviewer.protocol.session.ReceivedItem
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A received item in the Items tab. */
data class ItemRow(
    val item: ReceivedItem,
    val itemClass: ItemClass,
    /** The slot found the item in its own world. */
    val fromSelf: Boolean,
    /** The item was not seen in the Items tab before the user opened its slot. */
    val isNew: Boolean,
)

data class ItemsState(
    val connected: Boolean = false,
    val slotNames: List<String> = emptyList(),
    /** Number of new items of each slot, by slot name. */
    val newCounts: Map<String, Int> = emptyMap(),
    val slot: String? = null,
    /** Items of [slot], newest first. */
    val rows: List<ItemRow> = emptyList(),
    /** Number of new items of [slot]. */
    val newCount: Int = 0,
    /** Number of new items of all of the user's slots. */
    val unseenCount: Int = 0,
)

/**
 * Items the user's slots received in one multiworld. Items count as new if they arrived while the
 * app was connected and the user has not seen them in the Items tab. Items shown in the tab count
 * as seen only once the user leaves the slot, so the counters and markers stay while it is shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModel(private val container: AppContainer, private val multiworldId: String) :
    ViewModel() {
    private val slot = MutableStateFlow<String?>(null)

    /** Slot shown in the tab and the newest item index shown, saved as seen on leaving it. */
    private var shown: Pair<String, Int>? = null

    private val savedSlots: Flow<List<SavedSlot>> = container.profiles.profiles
        .map { it.multiworld(multiworldId)?.slots.orEmpty() }
        .distinctUntilChanged()

    private val itemsBySlot: Flow<Map<String, List<ReceivedItem>>> = savedSlots
        .map { slots -> slots.map { it.name } }
        .distinctUntilChanged()
        .flatMapLatest { names ->
            if (names.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    names.map { name ->
                        container.sessions.items(multiworldId, name).map { name to it }
                    },
                ) { it.toMap() }
            }
        }

    val state: StateFlow<ItemsState> = combine(
        savedSlots,
        container.sessions.status(multiworldId),
        itemsBySlot,
        slot,
    ) { slots, status, items, selectedSlot ->
        val slotNames = slots.map { it.name }
        val selected = selectedSlot?.takeIf { it in slotNames } ?: slotNames.firstOrNull()
        val seen = slots.firstOrNull { it.name == selected }?.lastSeenItemIndex ?: -1
        val newCounts = slots.associate { saved ->
            saved.name to items[saved.name].orEmpty().count { it.isUnseen(saved.lastSeenItemIndex) }
        }
        val rows = selected?.let { items[it] }.orEmpty().asReversed().map { item ->
            ItemRow(
                item = item,
                itemClass = ItemClass.of(item.item.flags),
                fromSelf = item.senderName == selected,
                isNew = item.isUnseen(seen),
            )
        }
        ItemsState(
            connected = status is MultiworldStatus.Connected,
            slotNames = slotNames,
            newCounts = newCounts,
            slot = selected,
            rows = rows,
            newCount = rows.count { it.isNew },
            unseenCount = newCounts.values.sum(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ItemsState())

    fun setSlot(value: String) {
        slot.value = value
    }

    /**
     * Notes that the tab shows the items of [slotName] up to [index]. They are saved as seen when
     * the user leaves the slot, by picking another one or with [leave].
     */
    fun markSeen(slotName: String, index: Int) {
        val previous = shown
        if (previous != null && previous.first != slotName) save(previous)
        shown = slotName to index
    }

    /** Called when the tab is hidden: the items shown so far are saved as seen. */
    fun leave() {
        shown?.let(::save)
        shown = null
    }

    override fun onCleared() {
        leave()
    }

    /** Saves in the app scope, so the write also finishes when this view model is cleared. */
    private fun save(seen: Pair<String, Int>) {
        val (slotName, index) = seen
        container.scope.launch {
            container.profiles.update { profiles ->
                val multiworld = profiles.multiworld(multiworldId) ?: return@update profiles
                val saved = multiworld.slots.firstOrNull { it.name == slotName }
                    ?: return@update profiles
                if (index <= saved.lastSeenItemIndex) return@update profiles
                profiles.withMultiworld(
                    multiworld.copy(
                        slots = multiworld.slots.map {
                            if (it.name == slotName) it.copy(lastSeenItemIndex = index) else it
                        },
                    ),
                )
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Only items that arrived while connected can be new; the server keeps no arrival times. */
private fun ReceivedItem.isUnseen(lastSeenIndex: Int): Boolean =
    receivedAtMillis != null && index > lastSeenIndex
