package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.model.HintStatus
import io.github.d4rthr4tz3.apviewer.protocol.monitor.FeedEntry
import io.github.d4rthr4tz3.apviewer.protocol.store.DataPackageCache
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemIndexStore
import io.github.d4rthr4tz3.apviewer.protocol.store.Profiles
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

/** Receives the feed messages of running sessions, for example to store them. */
fun interface FeedSink {
    suspend fun append(multiworldId: String, entries: List<FeedEntry>)
}

/**
 * Runs one [MultiworldSession] per saved multiworld that should be connected: those the user
 * connected, or that connect on start, while connections are enabled. Restarts a session when its
 * settings change and passes new feed messages to [feedSink].
 */
class SessionManager(
    private val scope: CoroutineScope,
    private val socketFactory: PacketSocketFactory,
    private val itemIndexStore: ItemIndexStore,
    private val dataPackageCache: DataPackageCache,
    private val feedSink: FeedSink,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    private val uuid: String = UUID.randomUUID().toString(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Running(
        val config: MultiworldConfig,
        val session: MultiworldSession,
        val job: Job,
    )

    private val running = mutableMapOf<String, Running>()
    private val _sessions = MutableStateFlow<Map<String, MultiworldSession>>(emptyMap())
    private val _wanted = MutableStateFlow<Set<String>?>(null)
    private var profiles = Profiles()

    /** Running sessions by multiworld id. */
    val sessions: StateFlow<Map<String, MultiworldSession>> = _sessions.asStateFlow()

    /** Ids of the multiworlds the user wants connected; `null` until the first [update]. */
    val wanted: StateFlow<Set<String>?> = _wanted.asStateFlow()

    /**
     * Applies changed [profiles]. The first call connects the multiworlds that connect on start;
     * later calls keep the user's choice and only restart sessions whose settings changed.
     */
    @Synchronized
    fun update(profiles: Profiles) {
        this.profiles = profiles
        val ids = profiles.multiworlds.map { it.id }.toSet()
        _wanted.value = _wanted.value?.intersect(ids)
            ?: profiles.multiworlds.filter { it.connectOnStart }.map { it.id }.toSet()
        reconcile()
    }

    /** Connects the multiworld with [id], or reconnects it if it is already running. */
    @Synchronized
    fun connect(id: String) {
        _wanted.value = _wanted.value.orEmpty() + id
        running.remove(id)?.stop()
        reconcile()
    }

    /** Disconnects the multiworld with [id] until it is connected again. */
    @Synchronized
    fun disconnect(id: String) {
        _wanted.value = _wanted.value.orEmpty() - id
        reconcile()
    }

    /** Sends [text] to the room of multiworld [id] as [slotName]. Returns `false` if not sent. */
    fun say(id: String, slotName: String, text: String): Boolean =
        _sessions.value[id]?.say(slotName, text) ?: false

    /** Hints of the multiworld with [id]; empty while it is not running. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun hints(id: String): Flow<List<MultiworldHint>> = sessions
        .map { it[id] }
        .distinctUntilChanged()
        .flatMapLatest { session -> session?.hints ?: flowOf(emptyList()) }
        .distinctUntilChanged()

    /** Sets the [status] of [hint] in multiworld [id]. Returns `false` if not sent. */
    fun updateHint(id: String, hint: SlotHint, status: HintStatus): Boolean =
        _sessions.value[id]?.updateHint(hint, status) ?: false

    /** Combined connection state of the multiworld with [id]. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun status(id: String): Flow<MultiworldStatus> = sessions
        .map { it[id] }
        .distinctUntilChanged()
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList<String>() to emptyList())
            } else {
                val names = session.slots.map { it.config.slotName }
                combine(session.slots.map { it.state }) { names to it.toList() }
            }
        }
        .scan<Pair<List<String>, List<SlotState>>, MultiworldStatus>(
            MultiworldStatus.Disconnected,
        ) { previous, (names, states) -> MultiworldStatus.of(names, states, previous, clock()) }
        .drop(1)
        .distinctUntilChanged()

    private fun reconcile() {
        val wanted = _wanted.value.orEmpty()
        val desired = profiles.multiworlds
            .filter {
                profiles.settings.connectionsEnabled && it.id in wanted &&
                    it.slots.isNotEmpty()
            }
            .associate { it.id to it.toConfig() }
        (running.keys - desired.keys).toList().forEach { id -> running.remove(id)?.stop() }
        desired.forEach { (id, config) ->
            val current = running[id]
            if (current?.config != config) {
                current?.stop()
                running[id] = start(id, config)
            }
        }
        _sessions.value = running.mapValues { it.value.session }
    }

    private fun start(id: String, config: MultiworldConfig): Running {
        val session = MultiworldSession(
            scope = scope,
            socketFactory = socketFactory,
            config = config,
            itemIndexStore = itemIndexStore,
            dataPackageCache = dataPackageCache,
            reconnectPolicy = reconnectPolicy,
            uuid = uuid,
            clock = clock,
        )
        // Started before the session so that no message of the first frame is missed.
        val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var storedId = -1L
            session.feed.collect { entries ->
                val newEntries = entries.filter { it.id > storedId }
                if (newEntries.isNotEmpty()) {
                    storedId = newEntries.last().id
                    feedSink.append(id, newEntries)
                }
            }
        }
        session.start()
        return Running(config, session, job)
    }

    private fun Running.stop() {
        session.stop()
        job.cancel()
    }
}
