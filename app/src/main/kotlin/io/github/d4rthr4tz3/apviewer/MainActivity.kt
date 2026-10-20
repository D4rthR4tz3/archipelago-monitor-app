package io.github.d4rthr4tz3.apviewer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.d4rthr4tz3.apviewer.notify.MonitorService
import io.github.d4rthr4tz3.apviewer.ui.ApApp
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Multiworld whose feed a tapped notification asked to open. */
    private val openFeed = MutableStateFlow<String?>(null)

    private val container get() = (application as ApViewerApplication).container

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so system bar icons stay light regardless of the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (savedInstanceState == null) openFeed.value = intent.multiworldId
        setContent {
            val feedToOpen by openFeed.collectAsStateWithLifecycle()
            ApViewerTheme {
                ApApp(openFeed = feedToOpen, onFeedOpened = { openFeed.value = null })
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // A foreground service may only be started while the app is visible.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.monitorStatus
                    .map { it != null }
                    .distinctUntilChanged()
                    .collect { needed -> if (needed) MonitorService.start(this@MainActivity) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.multiworldId?.let { openFeed.value = it }
    }

    override fun onStart() {
        super.onStart()
        container.setAppVisible(true)
        // Connections may have broken while the app was in the background; retry them right away.
        container.sessions.retryNow()
    }

    override fun onStop() {
        super.onStop()
        // A rotation stops the activity only to start it again at once.
        if (!isChangingConfigurations) container.setAppVisible(false)
    }

    private val Intent.multiworldId: String? get() = getStringExtra(EXTRA_MULTIWORLD_ID)

    companion object {
        /** Id of the multiworld whose feed to open. */
        const val EXTRA_MULTIWORLD_ID = "multiworld_id"
    }
}
