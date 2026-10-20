package io.github.d4rthr4tz3.apviewer.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.d4rthr4tz3.apviewer.AppContainer
import io.github.d4rthr4tz3.apviewer.protocol.message.FeedFilter
import io.github.d4rthr4tz3.apviewer.protocol.message.matches
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import io.github.d4rthr4tz3.apviewer.protocol.store.FeedLayout
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedState(
    val loaded: Boolean = false,
    val name: String = "",
    val slotNames: List<String> = emptyList(),
    val status: MultiworldStatus = MultiworldStatus.Disconnected,
    val layout: FeedLayout = FeedLayout.CLASSIC,
    val entries: List<FeedEntry> = emptyList(),
    val filter: FeedFilter = FeedFilter.ALL,
    val itemClasses: Set<ItemClass> = ItemClass.entries.toSet(),
    val sendAs: String? = null,
)

/** Feed of one multiworld: stored history, live state and the chat input. */
class FeedViewModel(private val container: AppContainer, private val multiworldId: String) :
    ViewModel() {
    private val filter = MutableStateFlow(FeedFilter.ALL)
    private val itemClasses = MutableStateFlow(ItemClass.entries.toSet())
    private val sendAs = MutableStateFlow<String?>(null)
    private val multiworld = container.profiles.profiles.map { it.multiworld(multiworldId) }

    private val options = combine(filter, itemClasses, sendAs, ::Triple)

    val state: StateFlow<FeedState> = combine(
        container.profiles.profiles,
        container.sessions.status(multiworldId),
        container.feed.observe(multiworldId),
        options,
    ) { profiles, status, entries, (quickFilter, classes, selectedSlot) ->
        val multiworld = profiles.multiworld(multiworldId)
        val slotNames = multiworld?.slots?.map { it.name }.orEmpty()
        FeedState(
            loaded = true,
            name = multiworld?.displayName.orEmpty(),
            slotNames = slotNames,
            status = status,
            layout = profiles.settings.feedLayout,
            entries = entries.filter { it.message.matches(quickFilter, classes) },
            filter = quickFilter,
            itemClasses = classes,
            sendAs = selectedSlot?.takeIf { it in slotNames } ?: slotNames.firstOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FeedState())

    init {
        // Everything shown while this screen is open counts as seen.
        viewModelScope.launch {
            container.feed.observeLatestId(multiworldId).filterNotNull().collect { latest ->
                val seen = multiworld.first()?.lastSeenFeedId ?: return@collect
                if (latest > seen) {
                    container.profiles.update { profiles ->
                        val current = profiles.multiworld(multiworldId) ?: return@update profiles
                        profiles.withMultiworld(
                            current.copy(lastSeenFeedId = maxOf(latest, current.lastSeenFeedId)),
                        )
                    }
                }
            }
        }
    }

    fun setFilter(value: FeedFilter) {
        filter.value = value
    }

    fun setItemClasses(value: Set<ItemClass>) {
        itemClasses.value = value
    }

    fun setSendAs(slotName: String) {
        sendAs.value = slotName
    }

    /** Sends [text] as the selected slot. Returns `false` if it could not be sent. */
    fun say(text: String): Boolean {
        val slot = state.value.sendAs ?: return false
        return text.isNotBlank() && container.sessions.say(multiworldId, slot, text.trim())
    }

    fun connect() = container.sessions.connect(multiworldId)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
