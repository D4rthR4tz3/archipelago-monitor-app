package io.github.d4rthr4tz3.apviewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.d4rthr4tz3.apviewer.ApViewerApplication
import io.github.d4rthr4tz3.apviewer.AppContainer
import io.github.d4rthr4tz3.apviewer.protocol.monitor.ArchipelagoMonitor
import io.github.d4rthr4tz3.apviewer.protocol.monitor.ConnectionConfig
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.monitor.MonitorState
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedMultiworld
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedSlot
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the [ArchipelagoMonitor] of the app for the lifetime of the activity. Until the overview
 * screen exists, the app works with one saved multiworld: the last one connected to.
 */
class MonitorViewModel(private val container: AppContainer) : ViewModel() {
    private val monitor = ArchipelagoMonitor(
        scope = viewModelScope,
        socketFactory = container.socketFactory,
        itemIndexStore = container.itemIndexStore,
        dataPackageCache = container.dataPackageCache,
    )

    /** Id of the multiworld the feed is stored for; set on connect. */
    private var multiworldId: String? = null

    /** Highest feed entry id of the current connection that is already stored. */
    private var storedFeedId = -1L

    val state: StateFlow<MonitorState> = monitor.state
    val feed: StateFlow<List<FeedEntry>> = monitor.feed

    /** The last connection, to prefill the connect form; `null` while loading or if none. */
    val lastConnection: StateFlow<ConnectionConfig?> = container.profiles.profiles
        .map { profiles -> profiles.multiworlds.firstOrNull()?.toConnectionConfig() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val settings = container.profiles.profiles.first().settings
            container.feed.prune(settings.feedRetentionDays)
        }
        viewModelScope.launch {
            monitor.feed.collect { entries -> storeNewEntries(entries) }
        }
    }

    fun connect(config: ConnectionConfig) {
        storedFeedId = -1L
        monitor.connect(config)
        viewModelScope.launch {
            val saved = container.profiles.profiles.first().multiworlds.firstOrNull()
            val multiworld = SavedMultiworld(
                id = saved?.id ?: UUID.randomUUID().toString(),
                address = config.address.trim(),
                roomPassword = config.password,
                slots = listOf(SavedSlot(name = config.slotName)),
            )
            multiworldId = multiworld.id
            container.profiles.saveMultiworld(multiworld)
        }
    }

    fun disconnect() = monitor.disconnect()

    private suspend fun storeNewEntries(entries: List<FeedEntry>) {
        val id = multiworldId ?: return
        val newEntries = entries.filter { it.id > storedFeedId }
        if (newEntries.isEmpty()) return
        storedFeedId = newEntries.last().id
        container.feed.append(id, newEntries)
    }

    private fun SavedMultiworld.toConnectionConfig(): ConnectionConfig = ConnectionConfig(
        address = address,
        slotName = slots.firstOrNull()?.name.orEmpty(),
        password = roomPassword,
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as ApViewerApplication
                MonitorViewModel(application.container)
            }
        }
    }
}
