package io.github.d4rthr4tz3.apviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.d4rthr4tz3.apviewer.ui.MonitorApp
import io.github.d4rthr4tz3.apviewer.ui.theme.ApViewerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApViewerTheme {
                MonitorApp()
            }
        }
    }
}
