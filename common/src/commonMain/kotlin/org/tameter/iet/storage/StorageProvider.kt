package org.tameter.iet.storage

/**
 * Interface for platform-specific storage operations.
 */
interface StorageProvider {
    /**
     * Returns the default directory for saving IET files (e.g., "Documents" folder).
     */
    fun getDefaultSaveDir(): String

    /**
     * Returns the last used save directory, or null if not set.
     */
    fun getLastUsedSaveDir(): String?

    /**
     * Persists the last used save directory.
     */
    fun setLastUsedSaveDir(path: String)
}
