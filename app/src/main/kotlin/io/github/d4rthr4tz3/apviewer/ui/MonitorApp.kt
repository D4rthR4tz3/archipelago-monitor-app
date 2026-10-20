package io.github.d4rthr4tz3.apviewer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.d4rthr4tz3.apviewer.protocol.monitor.MonitorState
import io.github.d4rthr4tz3.apviewer.ui.connect.ConnectScreen
import io.github.d4rthr4tz3.apviewer.ui.feed.FeedScreen

/** Root composable: shows the connect form until a slot is connected, then its feed. */
@Composable
fun MonitorApp(viewModel: MonitorViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()

    when (val current = state) {
        is MonitorState.Connected -> FeedScreen(
            state = current,
            feed = feed,
            onDisconnect = viewModel::disconnect,
        )

        else -> ConnectScreen(state = current, onConnect = viewModel::connect)
    }
}
