package io.github.d4rthr4tz3.apviewer.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.d4rthr4tz3.apviewer.ApViewerApplication
import kotlinx.coroutines.launch

/** Handles the actions of notifications: pausing all connections and dismissing a notification. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ApViewerApplication).container
        when (intent.action) {
            ACTION_PAUSE -> {
                val result = goAsync()
                container.scope.launch {
                    try {
                        container.profiles.updateSettings { it.copy(connectionsEnabled = false) }
                    } finally {
                        result.finish()
                    }
                }
            }

            ACTION_DISMISSED -> container.notifier.dismissed(
                multiworldId = intent.getStringExtra(EXTRA_MULTIWORLD_ID) ?: return,
                kind = intent.getStringExtra(EXTRA_KIND) ?: return,
            )
        }
    }

    companion object {
        private const val ACTION_PAUSE = "io.github.d4rthr4tz3.apviewer.action.PAUSE"
        private const val ACTION_DISMISSED = "io.github.d4rthr4tz3.apviewer.action.DISMISSED"
        private const val EXTRA_MULTIWORLD_ID = "multiworld_id"
        private const val EXTRA_KIND = "kind"

        /** Turns off all connections, like the switch on the overview. */
        fun pause(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NotificationActionReceiver::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE,
        )

        fun dismissed(
            context: Context,
            id: Int,
            multiworldId: String,
            kind: String,
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(ACTION_DISMISSED)
                .putExtra(EXTRA_MULTIWORLD_ID, multiworldId)
                .putExtra(EXTRA_KIND, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
