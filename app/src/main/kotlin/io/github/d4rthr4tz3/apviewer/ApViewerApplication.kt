package io.github.d4rthr4tz3.apviewer

import android.app.Application

class ApViewerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
