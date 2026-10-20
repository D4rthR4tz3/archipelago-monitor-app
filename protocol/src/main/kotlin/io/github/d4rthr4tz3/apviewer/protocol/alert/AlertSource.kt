package io.github.d4rthr4tz3.apviewer.protocol.alert

import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.session.MultiworldSession
import io.github.d4rthr4tz3.apviewer.protocol.session.ReceivedItem
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotHint
import io.github.d4rthr4tz3.apviewer.protocol.session.SlotState
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.protocol.store.NotifyMode
import io.github.d4rthr4tz3.apviewer.protocol.store.Profiles
import io.github.d4rthr4tz3.apviewer.protocol.store.SavedSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/** Something one of the user's slots may want a notification for. */
sealed interface Alert {
    val multiworldId: String

    /** [slotName] received a new [item]; [mode] says how to notify and is never `OFF`. */
    data class Item(
        override val multiworldId: String,
        val slotName: String,
        val item: ReceivedItem,
        val itemClass: ItemClass,
        val mode: NotifyMode,
    ) : Alert

    /** Someone hinted an item [slotName] receives. */
    data class NewHint(
        override val multiworldId: String,
        val slotName: String,
        val hint: SlotHint,
    ) : Alert

    /** A chat [message] from someone else names [slotNames]. */
    data class Mention(
        override val multiworldId: String,
        val slotNames: List<String>,
        val message: FeedMessage,
    ) : Alert
}

/**
 * Turns what the running [sessions] receive into [Alert]s, as far as the notification settings of
 * each slot in [profiles] allow. Only events that happen while collecting count.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertSource(
    private val sessions: StateFlow<Map<String, MultiworldSession>>,
    private val profiles: StateFlow<Profiles>,
) {
    val alerts: Flow<Alert> = sessions.flatMapLatest { running ->
        running.map { (id, session) -> alertsOf(id, session) }.merge()
    }

    private fun alertsOf(id: String, session: MultiworldSession): Flow<Alert> = merge(
        session.receivedItems.mapNotNull { (slotName, item) -> itemAlert(id, slotName, item) },
        session.slots.map { slot ->
            slot.newHints.mapNotNull { hint ->
                val slotName = slot.config.slotName
                hint.takeIf { savedSlot(id, slotName)?.notifications?.hints == true }
                    ?.let { Alert.NewHint(id, slotName, it) }
            }
        }.merge(),
        mentions(id, session),
    )

    private fun itemAlert(id: String, slotName: String, item: ReceivedItem): Alert? {
        if (!item.isNew) return null
        val notifications = savedSlot(id, slotName)?.notifications ?: return null
        if (item.senderName == slotName && !notifications.ownWorld) return null
        val itemClass = ItemClass.of(item.item.flags)
        val mode = notifications.mode(itemClass)
        if (mode == NotifyMode.OFF) return null
        return Alert.Item(id, slotName, item, itemClass, mode)
    }

    /** Chat messages that arrive from now on and name one of the user's slots. */
    private fun mentions(id: String, session: MultiworldSession): Flow<Alert> = flow {
        var lastId = session.feed.value.lastOrNull()?.id ?: -1L
        session.feed.collect { entries ->
            val newEntries = entries.filter { it.id > lastId }
            lastId = entries.lastOrNull()?.id ?: lastId
            val ownSlots = session.slots.mapNotNull {
                (it.state.value as? SlotState.Connected)?.info?.slot
            }
            newEntries.map { it.message }
                .filter { it.type == CHAT && it.sendingSlot !in ownSlots }
                .forEach { message ->
                    val text = message.plainText.substringAfter(": ")
                    val named = session.config.slots.map { it.name }.filter { name ->
                        savedSlot(id, name)?.notifications?.chatMentions == true &&
                            mentions(text, name)
                    }
                    if (named.isNotEmpty()) emit(Alert.Mention(id, named, message))
                }
        }
    }

    private fun savedSlot(id: String, slotName: String): SavedSlot? =
        profiles.value.multiworld(id)?.slots?.firstOrNull { it.name == slotName }

    private companion object {
        const val CHAT = "Chat"
    }
}

/** Whether [text] contains [name] as a whole word, ignoring case. */
internal fun mentions(text: String, name: String): Boolean = Regex(
    "(?<![\\p{L}\\p{N}_])${Regex.escape(name)}(?![\\p{L}\\p{N}_])",
    RegexOption.IGNORE_CASE,
).containsMatchIn(text)
