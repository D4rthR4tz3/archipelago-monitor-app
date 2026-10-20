package io.github.d4rthr4tz3.apviewer.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.d4rthr4tz3.apviewer.AppContainer
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A multiworld in the overview. */
data class MultiworldRow(
    val id: String,
    val name: String,
    val slotNames: List<String>,
    val status: MultiworldStatus,
    /** Items for the user received since the feed was last opened. */
    val unread: Int,
    val latestForMe: FeedEntry?,
)

data class OverviewState(
    val loaded: Boolean = false,
    val rows: List<MultiworldRow> = emptyList(),
    val connectionsEnabled: Boolean = true,
) {
    val connectedSlots: Int get() = rows.sumOf {
        (it.status as? MultiworldStatus.Connected)?.slots
            ?: 0
    }
    val totalSlots: Int get() = rows.sumOf { it.slotNames.size }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<OverviewState> = container.profiles.profiles
        .flatMapLatest { profiles ->
            val rows = profiles.multiworlds.map { multiworld ->
                combine(
                    container.sessions.status(multiworld.id),
                    container.feed.observeCountForMe(multiworld.id, multiworld.lastSeenFeedId),
                    container.feed.observeLatestForMe(multiworld.id),
                ) { status, unread, latest ->
                    MultiworldRow(
                        id = multiworld.id,
                        name = multiworld.displayName,
                        slotNames = multiworld.slots.map { it.name },
                        status = status,
                        unread = unread,
                        latestForMe = latest,
                    )
                }
            }
            val connectionsEnabled = profiles.settings.connectionsEnabled
            if (rows.isEmpty()) {
                flowOf(OverviewState(loaded = true, connectionsEnabled = connectionsEnabled))
            } else {
                combine(rows) { OverviewState(true, it.toList(), connectionsEnabled) }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            OverviewState(),
        )

    fun setConnectionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.profiles.update {
                it.copy(settings = it.settings.copy(connectionsEnabled = enabled))
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
