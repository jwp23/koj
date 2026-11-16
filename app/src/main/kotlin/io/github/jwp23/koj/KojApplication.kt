package io.github.jwp23.koj

import android.app.Application
import io.github.jwp23.koj.storage.openKojDatabase

class KojApplication : Application() {
    val container: AppContainer by lazy { AppContainer(openKojDatabase(this)) }
}
