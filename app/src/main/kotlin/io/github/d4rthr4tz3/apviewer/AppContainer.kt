package io.github.d4rthr4tz3.apviewer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import io.github.d4rthr4tz3.apviewer.data.FeedRepository
import io.github.d4rthr4tz3.apviewer.data.ProfilesRepository
import io.github.d4rthr4tz3.apviewer.data.RoomDataPackageCache
import io.github.d4rthr4tz3.apviewer.data.RoomItemIndexStore
import io.github.d4rthr4tz3.apviewer.data.db.ApDatabase
import io.github.d4rthr4tz3.apviewer.notify.MonitorStatus
import io.github.d4rthr4tz3.apviewer.notify.Notifier
import io.github.d4rthr4tz3.apviewer.protocol.alert.AlertSource
import io.github.d4rthr4tz3.apviewer.protocol.client.OkHttpPacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldStatus
import io.github.d4rthr4tz3.apviewer.protocol.session.RoomProbe
import io.github.d4rthr4tz3.apviewer.protocol.session.SessionManager
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotState
import io.github.d4rthr4tz3.apviewer.protocol.store.Profiles
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * App-wide singletons, created once per process by [ApViewerApplication]. Connections live in
 * [scope], so they survive configuration changes and leaving a screen.
 */
class AppContainer(private val context: Context) {
    private val database = ApDatabase.create(context)

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val profiles: ProfilesRepository = ProfilesRepository.create(context)
    val feed: FeedRepository = FeedRepository(database.feedDao())

    private val socketFactory: PacketSocketFactory = OkHttpPacketSocket.Factory(
        OkHttpClient.Builder()
            .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .build(),
    )

    val sessions = SessionManager(
        scope = scope,
        socketFactory = socketFactory,
        itemIndexStore = RoomItemIndexStore(database.itemIndexDao()),
        dataPackageCache = RoomDataPackageCache(database.dataPackageDao()),
        feedSink = feed,
    )

    val probe = RoomProbe(socketFactory)

    private val profilesState: StateFlow<Profiles> =
        profiles.profiles.stateIn(scope, SharingStarted.Eagerly, Profiles())

    val notifier =
        Notifier(context, CoroutineScope(scope.coroutineContext + Dispatchers.Main), profilesState)

    /**
     * What the ongoing notification shows while a multiworld kept in the background is running;
     * `null` while none is, so the background service is not needed.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val monitorStatus: Flow<MonitorStatus?> = combine(
        sessions.sessions.flatMapLatest { running ->
            if (running.isEmpty()) {
                flowOf(emptyList<RunningMultiworld>())
            } else {
                combine(
                    running.map { (id, session) ->
                        combine(session.slots.map { it.state }) { states ->
                            RunningMultiworld(
                                id = id,
                                slotNames = session.config.slots.map { it.name },
                                states = states.toList(),
                            )
                        }
                    },
                ) { it.toList() }
            }
        },
        profilesState,
    ) { running, profiles ->
        if (running.none { profiles.multiworld(it.id)?.keepInBackground == true }) {
            return@combine null
        }
        MonitorStatus(
            connectedSlots = running.sumOf { mw -> mw.states.count { it is SlotState.Connected } },
            multiworlds = running.size,
            problems = running.mapNotNull { mw ->
                val name = profiles.multiworld(mw.id)?.displayName ?: return@mapNotNull null
                problemText(
                    name,
                    MultiworldStatus.of(mw.slotNames, mw.states, MultiworldStatus.Disconnected, 0L),
                )
            },
        )
    }.distinctUntilChanged()

    init {
        notifier.createChannels()
        // Sessions in the background start only once the app says it is in the background.
        sessions.setForeground(false)
        scope.launch(Dispatchers.Main) {
            AlertSource(sessions.sessions, profilesState).alerts.collect(notifier::onAlert)
        }
        scope.launch {
            feed.prune(profiles.profiles.first().multiworlds)
            profiles.profiles.collect { sessions.update(it) }
        }
        // Without a network, slots wait instead of retrying; once it is back, they connect at once.
        context.getSystemService(ConnectivityManager::class.java)?.let { connectivity ->
            sessions.setOnline(connectivity.activeNetwork != null)
            connectivity.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    private var current: Network? = null

                    override fun onAvailable(network: Network) {
                        current = network
                        sessions.setOnline(true)
                    }

                    // activeNetwork may still name the lost network here, so only the callbacks
                    // count. A loss reported after a switch to another network changes nothing.
                    override fun onLost(network: Network) {
                        if (network != current) return
                        current = null
                        sessions.setOnline(false)
                    }
                },
            )
        }
    }

    /** Tells whether the app is visible: only then do all wanted multiworlds stay connected. */
    fun setAppVisible(visible: Boolean) {
        notifier.appVisible = visible
        sessions.setForeground(visible)
    }

    private fun problemText(name: String, status: MultiworldStatus): String? = when (status) {
        is MultiworldStatus.Connected, MultiworldStatus.Disconnected -> null

        MultiworldStatus.Connecting -> context.getString(R.string.notify_status_connecting, name)

        is MultiworldStatus.Reconnecting ->
            context.getString(R.string.notify_status_reconnecting, name)

        MultiworldStatus.Offline -> context.getString(R.string.notify_status_offline, name)

        is MultiworldStatus.Failed -> context.getString(R.string.notify_status_failed, name)
    }

    private class RunningMultiworld(
        val id: String,
        val slotNames: List<String>,
        val states: List<SlotState>,
    )

    private companion object {
        const val PING_INTERVAL_SECONDS = 30L
    }
}
