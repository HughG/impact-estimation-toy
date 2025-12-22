package org.tameter.iet.storage

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmStorageProviderTest {
    private val provider = JvmStorageProvider()

    @Test
    fun getDefaultSaveDir_returns_path_containing_Documents() {
        // Given/When
        val defaultDir = provider.getDefaultSaveDir()
        
        // Then
        assertNotNull(defaultDir)
        assertTrue(defaultDir.endsWith("Documents"), "Expected path to end with 'Documents', but was: $defaultDir")
    }

    @Test
    fun can_persist_and_retrieve_last_used_dir() {
        // Given
        val testDir = "C:\\Users\\Test\\Documents"
        
        // When
        provider.setLastUsedSaveDir(testDir)
        val retrievedDir = provider.getLastUsedSaveDir()
        
        // Then
        assertTrue(retrievedDir == testDir, "Expected $testDir but got $retrievedDir")
    }
}
