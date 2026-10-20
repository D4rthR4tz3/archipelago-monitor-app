package io.github.d4rthr4tz3.apviewer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.d4rthr4tz3.apviewer.AppContainer
import io.github.d4rthr4tz3.apviewer.R
import io.github.d4rthr4tz3.apviewer.protocol.store.AppSettings
import io.github.d4rthr4tz3.apviewer.protocol.store.FeedLayout
import io.github.d4rthr4tz3.apviewer.ui.components.ApIconButton
import io.github.d4rthr4tz3.apviewer.ui.components.ScreenHeader
import io.github.d4rthr4tz3.apviewer.ui.components.SectionTitle
import io.github.d4rthr4tz3.apviewer.ui.components.SegmentedControl
import io.github.d4rthr4tz3.apviewer.ui.containerViewModel
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.profiles.profiles
        .map { it.settings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppSettings())

    fun setFeedLayout(layout: FeedLayout) {
        viewModelScope.launch {
            container.profiles.update { it.copy(settings = it.settings.copy(feedLayout = layout)) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** App-wide settings. */
@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel { SettingsViewModel(it) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.settings_title),
            navigation = {
                ApIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            },
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_feed_layout))
            SegmentedControl(
                options = listOf(
                    FeedLayout.CLASSIC to stringResource(R.string.settings_layout_classic),
                    FeedLayout.STRUCTURED to stringResource(R.string.settings_layout_structured),
                ),
                selected = settings.feedLayout,
                onSelect = viewModel::setFeedLayout,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.settings_feed_layout_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ApColors.TextTertiary,
            )
        }
    }
}
