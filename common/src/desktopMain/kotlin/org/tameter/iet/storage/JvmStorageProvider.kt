package org.tameter.iet.storage

import java.io.File
import java.util.prefs.Preferences

/**
 * JVM implementation of StorageProvider for Desktop (Windows).
 */
class JvmStorageProvider : StorageProvider {
    private val prefs = Preferences.userNodeForPackage(JvmStorageProvider::class.java)
    private val LAST_DIR_KEY = "last_used_save_dir"

    override fun getDefaultSaveDir(): String {
        val userHome = System.getProperty("user.home")
        return File(userHome, "Documents").absolutePath
    }

    override fun getLastUsedSaveDir(): String? {
        return prefs.get(LAST_DIR_KEY, null)
    }

    override fun setLastUsedSaveDir(path: String) {
        prefs.put(LAST_DIR_KEY, path)
    }
}
