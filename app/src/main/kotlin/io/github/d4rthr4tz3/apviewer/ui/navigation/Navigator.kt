package io.github.d4rthr4tz3.apviewer.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** Screens of the app. */
sealed interface Destination {
    data object Overview : Destination

    data class Feed(val multiworldId: String) : Destination

    /** Edits the multiworld with [multiworldId], or creates a new one if it is `null`. */
    data class MultiworldEdit(val multiworldId: String?) : Destination

    /** Edits a slot of the draft held by the [MultiworldEdit] entry below it. */
    data class SlotSettings(val slotIndex: Int) : Destination

    data object Settings : Destination
}

/**
 * An entry of the back stack. It owns the view models of its screen, which are cleared when the
 * entry is popped.
 */
class NavEntry(val id: Long, val destination: Destination, val parent: NavEntry?) :
    ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

/** Back stack of the app, kept across configuration changes. */
class Navigator : ViewModel() {
    private var nextId = 0L

    val stack = mutableStateListOf(NavEntry(nextId++, Destination.Overview, parent = null))

    val current: NavEntry get() = stack.last()

    /** Opens [destination]; [parent] gives it access to the view models of an earlier entry. */
    fun push(destination: Destination, parent: NavEntry? = null) {
        stack += NavEntry(nextId++, destination, parent)
    }

    /** Closes the current screen. Returns `false` if it is the last one. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex).viewModelStore.clear()
        return true
    }

    /** Closes the current screen and opens [destination] in its place. */
    fun replace(destination: Destination) {
        val parent = current.parent
        pop()
        push(destination, parent)
    }

    override fun onCleared() {
        stack.forEach { it.viewModelStore.clear() }
    }
}
