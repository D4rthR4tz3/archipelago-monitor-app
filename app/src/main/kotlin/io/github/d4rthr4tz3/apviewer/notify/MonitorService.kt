package io.github.d4rthr4tz3.apviewer.notify

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.d4rthr4tz3.apviewer.ApViewerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process, and with it the connections of the multiworlds kept
 * in the background, alive while the app is not visible. Shows the connection status and stops
 * itself once no such multiworld is connected.
 */
class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var status = MonitorStatus(connectedSlots = 0, multiworlds = 0, problems = emptyList())
    private var watching: Job? = null

    private val container get() = (application as ApViewerApplication).container

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every start through startForegroundService needs a call to startForeground.
        ServiceCompat.startForeground(
            this,
            Notifier.STATUS_ID,
            container.notifier.status(status),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        // Started only now, as stopping before startForeground would crash the app.
        if (watching == null) {
            watching = scope.launch {
                container.monitorStatus.collect { current ->
                    if (current == null) {
                        stopSelf()
                    } else {
                        status = current
                        show()
                    }
                }
            }
        }
        return START_STICKY
    }

    // The ongoing notification of a foreground service needs no permission.
    @SuppressLint("MissingPermission")
    private fun show() {
        NotificationManagerCompat.from(this)
            .notify(Notifier.STATUS_ID, container.notifier.status(status))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Starts the service; only allowed while the app is visible. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MonitorService::class.java),
            )
        }
    }
}
