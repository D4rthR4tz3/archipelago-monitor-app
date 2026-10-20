package io.github.d4rthr4tz3.apviewer.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.d4rthr4tz3.apviewer.MainActivity
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.alert.Alert
import io.github.d4rthr4tz3.apviewer.protocol.store.ItemClass
import io.github.d4rthr4tz3.apviewer.protocol.store.NotifyMode
import io.github.d4rthr4tz3.apviewer.protocol.store.Profiles
import io.github.d4rthr4tz3.apviewer.ui.theme.ContentColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the ongoing notification of [MonitorService] shows. */
data class MonitorStatus(
    val connectedSlots: Int,
    val multiworlds: Int,
    /** One line per multiworld that is not fully connected, such as "Friday reconnecting". */
    val problems: List<String>,
)

/**
 * Posts the app's notifications: one per multiworld for items, hints and chat mentions, and the
 * ongoing status of [MonitorService]. Alerts that arrive close together are posted at once, so a
 * burst of items rings only once. Must be used from the main thread.
 */
class Notifier(
    private val context: Context,
    private val scope: CoroutineScope,
    private val profiles: StateFlow<Profiles>,
) {
    private enum class Kind { ITEMS, HINTS, MENTIONS }

    private data class Key(val multiworldId: String, val kind: Kind)

    private class Pending {
        /** Lines of the notification, newest first. */
        val lines = mutableListOf<Line>()
        var sound = false
        var job: Job? = null
    }

    private class Line(val title: CharSequence, val detail: CharSequence)

    private val manager = NotificationManagerCompat.from(context)
    private val pending = mutableMapOf<Key, Pending>()
    private val ids = mutableMapOf<Key, Int>()

    /** While the app is visible, alerts are dropped: the user sees them in the app. */
    var appVisible = false

    fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                channel(CHANNEL_STATUS, R.string.channel_status, IMPORTANCE_LOW),
                channel(CHANNEL_ITEMS, R.string.channel_items, IMPORTANCE_HIGH),
                channel(CHANNEL_ITEMS_SILENT, R.string.channel_items_silent, IMPORTANCE_LOW),
                channel(CHANNEL_HINTS, R.string.channel_hints, IMPORTANCE_DEFAULT),
                channel(CHANNEL_MENTIONS, R.string.channel_mentions, IMPORTANCE_HIGH),
            ),
        )
    }

    fun onAlert(alert: Alert) {
        if (appVisible) return
        when (alert) {
            is Alert.Item -> add(
                Key(alert.multiworldId, Kind.ITEMS),
                Line(
                    title = itemText(alert.item.itemName, alert.itemClass).append(' ').append(
                        context.getString(
                            R.string.notify_item_to,
                            alert.slotName,
                            alert.item.senderName,
                        ),
                    ),
                    detail = alert.item.locationName,
                ),
                sound = alert.mode == NotifyMode.SOUND,
            )

            is Alert.NewHint -> add(
                Key(alert.multiworldId, Kind.HINTS),
                Line(
                    title = itemText(alert.hint.itemName, ItemClass.of(alert.hint.itemFlags))
                        .append(' ')
                        .append(context.getString(R.string.notify_hint_for, alert.slotName)),
                    detail = context.getString(
                        R.string.notify_hint_at,
                        alert.hint.finderName,
                        alert.hint.locationName,
                    ),
                ),
                sound = true,
            )

            is Alert.Mention -> add(
                Key(alert.multiworldId, Kind.MENTIONS),
                Line(
                    title = context.getString(
                        R.string.notify_mention,
                        alert.slotNames.joinToString(", "),
                    ),
                    detail = alert.message.plainText,
                ),
                sound = true,
            )
        }
    }

    /** Removes the notifications of the multiworld with [multiworldId], for example when opened. */
    fun clear(multiworldId: String) {
        Kind.entries.forEach { kind ->
            val key = Key(multiworldId, kind)
            pending.remove(key)?.job?.cancel()
            ids[key]?.let(manager::cancel)
        }
    }

    /** Forgets the lines of a notification the user dismissed. */
    fun dismissed(multiworldId: String, kind: String) {
        val key = Key(multiworldId, Kind.valueOf(kind))
        pending.remove(key)?.job?.cancel()
    }

    /** The ongoing notification of [MonitorService]. */
    fun status(status: MonitorStatus): Notification {
        val text = context.getString(
            R.string.notify_status_connected,
            plural(R.plurals.slot_count, status.connectedSlots),
            plural(R.plurals.multiworld_count, status.multiworlds),
        )
        val detail = status.problems.joinToString("\n")
        return NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text)
            .setContentText(detail.ifEmpty { null })
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail.ifEmpty { null }))
            .setContentIntent(openApp(STATUS_ID, multiworldId = null))
            .addAction(
                0,
                context.getString(R.string.action_pause),
                NotificationActionReceiver.pause(context),
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun add(key: Key, line: Line, sound: Boolean) {
        val entry = pending.getOrPut(key, ::Pending)
        entry.lines.add(0, line)
        entry.sound = entry.sound || sound
        entry.job?.cancel()
        entry.job = scope.launch {
            delay(BATCH_DELAY_MILLIS)
            post(key, entry)
        }
    }

    // The permission is checked by canPost.
    @SuppressLint("MissingPermission")
    private fun post(key: Key, entry: Pending) {
        if (!canPost()) return
        val id = ids.getOrPut(key) { FIRST_ID + ids.size }
        val name = profiles.value.multiworld(key.multiworldId)?.displayName ?: return
        val lines = entry.lines
        val newest = lines.first()
        val channel = when (key.kind) {
            Kind.ITEMS -> if (entry.sound) CHANNEL_ITEMS else CHANNEL_ITEMS_SILENT
            Kind.HINTS -> CHANNEL_HINTS
            Kind.MENTIONS -> CHANNEL_MENTIONS
        }
        val count = lines.size
        val (title, text) = when {
            key.kind == Kind.HINTS -> plural(R.plurals.notify_hints, count) to newest.title

            count > 1 && key.kind == Kind.ITEMS -> plural(R.plurals.notify_items, count) to
                newest.title

            count > 1 -> plural(R.plurals.notify_mentions, count) to newest.title

            // A single mention: who is mentioned above, the message below.
            key.kind == Kind.MENTIONS -> newest.detail to newest.title

            else -> newest.title to newest.detail
        }
        val style = if (count == 1) {
            NotificationCompat.BigTextStyle().bigText(
                if (key.kind == Kind.HINTS) {
                    SpannableStringBuilder(newest.title).append('\n').append(newest.detail)
                } else {
                    text
                },
            )
        } else {
            NotificationCompat.InboxStyle().also { inbox ->
                lines.take(MAX_LINES).forEach { inbox.addLine(it.title) }
                if (count > MAX_LINES) {
                    inbox.setSummaryText(plural(R.plurals.notify_more, count - MAX_LINES))
                }
            }
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setSubText(name)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setNumber(count)
            .setSilent(!entry.sound)
            .setAutoCancel(true)
            .setContentIntent(openApp(id, key.multiworldId))
            .setDeleteIntent(
                NotificationActionReceiver.dismissed(context, id, key.multiworldId, key.kind.name),
            )
            .addAction(
                0,
                context.getString(R.string.notify_show_feed),
                openApp(id, key.multiworldId),
            )
            .build()
        entry.sound = false
        manager.notify(id, notification)
    }

    private fun plural(id: Int, count: Int): String =
        context.resources.getQuantityString(id, count, count)

    private fun canPost(): Boolean = manager.areNotificationsEnabled() &&
        (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) ==
                PackageManager.PERMISSION_GRANTED
            )

    private fun itemText(name: String, itemClass: ItemClass): SpannableStringBuilder {
        val (color, bold) = when (itemClass) {
            ItemClass.PROGRESSION -> ContentColors.Progression to true
            ItemClass.USEFUL -> ContentColors.Useful to false
            ItemClass.TRAP -> ContentColors.Trap to true
            ItemClass.FILLER -> ContentColors.Filler to false
        }
        return SpannableStringBuilder(name).apply {
            setSpan(
                ForegroundColorSpan(color.toArgb()),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            if (bold) setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun openApp(requestCode: Int, multiworldId: String?): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_MULTIWORLD_ID, multiworldId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun channel(id: String, name: Int, importance: Int) =
        NotificationChannelCompat.Builder(id, importance)
            .setName(context.getString(name))
            .build()

    companion object {
        const val STATUS_ID = 1
        private const val FIRST_ID = 100
        private const val BATCH_DELAY_MILLIS = 750L
        private const val MAX_LINES = 5

        private const val CHANNEL_STATUS = "status"
        private const val CHANNEL_ITEMS = "items"
        private const val CHANNEL_ITEMS_SILENT = "items_silent"
        private const val CHANNEL_HINTS = "hints"
        private const val CHANNEL_MENTIONS = "mentions"

        private const val IMPORTANCE_LOW = NotificationManagerCompat.IMPORTANCE_LOW
        private const val IMPORTANCE_DEFAULT = NotificationManagerCompat.IMPORTANCE_DEFAULT
        private const val IMPORTANCE_HIGH = NotificationManagerCompat.IMPORTANCE_HIGH
    }
}
