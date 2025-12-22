import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.tameter.common.App
import org.tameter.iet.di.ServiceLocator
import org.tameter.iet.storage.JvmStorageProvider
import org.tameter.iet.storage.StorageProvider


fun main() {
    // Register platform services
    ServiceLocator.register(StorageProvider::class.java, JvmStorageProvider())

    application {
        Window(onCloseRequest = ::exitApplication) {
            App()
        }
    }
}
