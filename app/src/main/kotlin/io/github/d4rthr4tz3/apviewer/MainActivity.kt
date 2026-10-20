package io.github.d4rthr4tz3.apviewer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.d4rthr4tz3.apviewer.ui.ApApp
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so system bar icons stay light regardless of the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            ApViewerTheme {
                ApApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Connections may have broken while the app was in the background; retry them right away.
        (application as ApViewerApplication).container.sessions.retryNow()
    }
}
