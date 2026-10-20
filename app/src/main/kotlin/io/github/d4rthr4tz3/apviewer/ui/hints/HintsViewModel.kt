package io.github.d4rthr4tz3.apviewer.ui.hints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.d4rthr4tz3.apviewer.AppContainer
import io.github.d4rthr4tz3.apviewer.protocol.model.HintStatus
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldHint
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Which side of a hint the user is on. */
enum class HintDirection { RECEIVE, FIND }

/** Columns of the hint table, each sortable. */
enum class HintColumn { ITEM, RECEIVER, LOCATION, FINDER, ENTRANCE, STATUS }

data class HintsState(
    val connected: Boolean = false,
    val slotNames: List<String> = emptyList(),
    /** Hints of the selected direction, slot and visibility, for the list. */
    val visible: List<MultiworldHint> = emptyList(),
    /** All hints, sorted by [sortColumn], for the table. */
    val table: List<MultiworldHint> = emptyList(),
    /** Number of hints the user receives with priority that are not found yet. */
    val priorityCount: Int = 0,
    val direction: HintDirection = HintDirection.RECEIVE,
    /** Slot to show, or `null` for all of the user's slots. */
    val slot: String? = null,
    val hideFound: Boolean = false,
    val sortColumn: HintColumn = HintColumn.STATUS,
)

/** Hints of one multiworld, with the list filters and the table sort order. */
class HintsViewModel(private val container: AppContainer, private val multiworldId: String) :
    ViewModel() {
    private val direction = MutableStateFlow(HintDirection.RECEIVE)
    private val slot = MutableStateFlow<String?>(null)
    private val hideFound = MutableStateFlow(false)
    private val sortColumn = MutableStateFlow(HintColumn.STATUS)

    private val options = combine(direction, slot, hideFound, sortColumn) { dir, sel, hide, sort ->
        Options(dir, sel, hide, sort)
    }

    val state: StateFlow<HintsState> = combine(
        container.profiles.profiles,
        container.sessions.status(multiworldId),
        container.sessions.hints(multiworldId),
        options,
    ) { profiles, status, hints, current ->
        val slotNames = profiles.multiworld(multiworldId)?.slots?.map { it.name }.orEmpty()
        val selected = current.slot?.takeIf { it in slotNames }
        val sorted = hints.sortedWith(compareBy { it.hint.status.sortOrder })
        HintsState(
            connected = status is MultiworldStatus.Connected,
            slotNames = slotNames,
            visible = sorted.filter { it.matches(current.direction, selected, current.hideFound) },
            table = hints.sortedWith(comparatorFor(current.sortColumn)),
            priorityCount = hints.count {
                it.receiverIsOwn && it.hint.status == HintStatus.PRIORITY
            },
            direction = current.direction,
            slot = selected,
            hideFound = current.hideFound,
            sortColumn = current.sortColumn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HintsState())

    fun setDirection(value: HintDirection) {
        direction.value = value
    }

    fun setSlot(value: String?) {
        slot.value = value
    }

    fun setHideFound(value: Boolean) {
        hideFound.value = value
    }

    fun setSortColumn(value: HintColumn) {
        sortColumn.value = value
    }

    private data class Options(
        val direction: HintDirection,
        val slot: String?,
        val hideFound: Boolean,
        val sortColumn: HintColumn,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Position of a status when sorting: priority first, found last. */
val HintStatus.sortOrder: Int
    get() = when (this) {
        HintStatus.PRIORITY -> 0
        HintStatus.UNSPECIFIED -> 1
        HintStatus.AVOID -> 2
        HintStatus.NO_PRIORITY -> 3
        HintStatus.FOUND -> 4
    }

private fun MultiworldHint.matches(
    direction: HintDirection,
    slot: String?,
    hideFound: Boolean,
): Boolean {
    val (own, name) = when (direction) {
        HintDirection.RECEIVE -> receiverIsOwn to hint.receiverName
        HintDirection.FIND -> finderIsOwn to hint.finderName
    }
    return own && (slot == null || name == slot) && !(hideFound && hint.status == HintStatus.FOUND)
}

/** Sorts by [column] ascending; empty entrances go last. */
private fun comparatorFor(column: HintColumn): Comparator<MultiworldHint> = when (column) {
    HintColumn.ITEM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.hint.itemName }

    HintColumn.RECEIVER -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.hint.receiverName }

    HintColumn.LOCATION -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.hint.locationName }

    HintColumn.FINDER -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.hint.finderName }

    HintColumn.ENTRANCE -> compareBy<MultiworldHint> { it.hint.entrance.isEmpty() }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.hint.entrance }

    HintColumn.STATUS -> compareBy { it.hint.status.sortOrder }
}
