package io.github.d4rthr4tz3.apviewer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.d4rthr4tz3.apviewer.ui.feed.FeedRoute
import io.github.d4rthr4tz3.apviewer.ui.multiworld.MultiworldEditScreen
import io.github.d4rthr4tz3.apviewer.ui.multiworld.MultiworldEditViewModel
import io.github.d4rthr4tz3.apviewer.ui.navigation.Destination
import io.github.d4rthr4tz3.apviewer.ui.navigation.NavEntry
import io.github.d4rthr4tz3.apviewer.ui.navigation.Navigator
import io.github.d4rthr4tz3.apviewer.ui.overview.OverviewRoute
import io.github.d4rthr4tz3.apviewer.ui.settings.SettingsRoute
import io.github.d4rthr4tz3.apviewer.ui.slot.SlotSettingsScreen
import io.github.d4rthr4tz3.apviewer.ui.theme.ApColors

/** Root of the app: shows the screen on top of the back stack. */
@Composable
fun ApApp(navigator: Navigator = viewModel()) {
    val entry = navigator.current
    val stateHolder = rememberSaveableStateHolder()
    BackHandler(enabled = navigator.stack.size > 1) { navigator.pop() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ApColors.Background)
            .safeDrawingPadding(),
    ) {
        stateHolder.SaveableStateProvider(entry.id) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides entry) {
                Screen(entry = entry, navigator = navigator)
            }
        }
    }
}

@Composable
private fun Screen(entry: NavEntry, navigator: Navigator) {
    when (val destination = entry.destination) {
        Destination.Overview -> OverviewRoute(
            onOpenFeed = { navigator.push(Destination.Feed(it)) },
            onAddMultiworld = { navigator.push(Destination.MultiworldEdit(null)) },
            onOpenSettings = { navigator.push(Destination.Settings) },
        )

        is Destination.Feed -> FeedRoute(
            multiworldId = destination.multiworldId,
            onBack = { navigator.pop() },
            onEdit = { navigator.push(Destination.MultiworldEdit(destination.multiworldId)) },
        )

        is Destination.MultiworldEdit -> {
            val viewModel = containerViewModel(owner = entry) {
                MultiworldEditViewModel(it, destination.multiworldId)
            }
            MultiworldEditScreen(
                viewModel = viewModel,
                onClose = { navigator.pop() },
                onSaved = {
                    if (viewModel.isNew) {
                        navigator.replace(Destination.Feed(viewModel.id))
                    } else {
                        navigator.pop()
                    }
                },
                onRemoved = {
                    while (navigator.current.destination != Destination.Overview) navigator.pop()
                },
                onOpenSlot = { index ->
                    navigator.push(Destination.SlotSettings(index), parent = entry)
                },
            )
        }

        is Destination.SlotSettings -> {
            val parent = checkNotNull(entry.parent) { "Slot settings need the multiworld draft" }
            val parentDestination = parent.destination as Destination.MultiworldEdit
            val viewModel = containerViewModel(owner = parent) {
                MultiworldEditViewModel(it, parentDestination.multiworldId)
            }
            SlotSettingsScreen(
                viewModel = viewModel,
                slotIndex = destination.slotIndex,
                onBack = { navigator.pop() },
            )
        }

        Destination.Settings -> SettingsRoute(onBack = { navigator.pop() })
    }
}
