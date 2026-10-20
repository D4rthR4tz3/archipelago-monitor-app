package io.github.d4rthr4tz3.apviewer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.d4rthr4tz3.apviewer.ApViewerApplication
import io.github.d4rthr4tz3.apviewer.AppContainer

/** Returns the [VM] of [owner], creating it with [create] from the app's [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as ApViewerApplication).container
    return viewModel(
        viewModelStoreOwner = owner,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
