@file:Suppress("DestructuringDeclaration", "MoveLambdaOutsideParentheses")

package io.github.cfsima.modernsafe

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var repoScope: CoroutineScope

    @Before
    fun setUp() {
        repoScope = CoroutineScope(SupervisorJob() + testDispatcher)
    }

    @After
    fun tearDown() {
        repoScope.cancel()
    }

    private fun createRepository(): Pair<SettingsRepository, File> {
        val testFile = tmpFolder.newFile("test_settings_${System.nanoTime()}.preferences_pb")
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = repoScope,
            produceFile = { testFile },
        )

        val filesDir = tmpFolder.newFolder("files_${System.nanoTime()}")
        val dummyContext = object : android.content.ContextWrapper(null) {
            override fun getPackageName(): String = "io.github.cfsima.modernsafe"
            override fun getFilesDir(): File = filesDir
            override fun getExternalFilesDir(type: String?): File = filesDir
            override fun getApplicationContext(): Context = this
        }

        val repository = SettingsRepository(
            context = dummyContext,
            dataStore = dataStore,
            externalScope = repoScope,
        )
        return Pair(repository, testFile)
    }

    @Test
    fun testDefaultSettings() = testScope.runTest {
        val (repository, _) = createRepository()
        val settings = repository.settingsFlow.first()

        assertFalse("externalAccess default should be false", settings.externalAccess)
        assertEquals("lockTimeout default should be 5", "5", settings.lockTimeout)
        assertTrue("lockOnScreenLock default should be true", settings.lockOnScreenLock)
        assertFalse("keypad default should be false", settings.keypad)
        assertFalse("keypadMute default should be false", settings.keypadMute)
        assertTrue("autoBackup default should be true", settings.autoBackup)
        assertEquals("autoBackupDays default should be 7", "7", settings.autoBackupDays)
        assertEquals(Settings.IO_METHOD_FILE, settings.backupMethod)
        assertEquals(Settings.IO_METHOD_FILE, settings.exportMethod)
    }

    @Test
    fun testUpdateExternalAccess() = testScope.runTest {
        val (repository, _) = createRepository()

        repository.setExternalAccess(enabled = true)
        val settings = repository.settingsFlow.first()

        assertTrue(settings.externalAccess)
    }

    @Test
    fun testUpdateLockTimeout() = testScope.runTest {
        val (repository, _) = createRepository()

        repository.setLockTimeout("15")
        val settings = repository.settingsFlow.first()

        assertEquals("15", settings.lockTimeout)
    }

    @Test
    fun testUpdateKeypadAndMute() = testScope.runTest {
        val (repository, _) = createRepository()

        repository.setKeypad(enabled = true)
        repository.setKeypadMute(enabled = true)
        val settings = repository.settingsFlow.first()

        assertTrue(settings.keypad)
        assertTrue(settings.keypadMute)
    }

    @Test
    fun testUpdateBackupDocumentAndMethod() = testScope.runTest {
        val (repository, _) = createRepository()
        val testUri = "content://com.android.providers.documents/document/123"

        repository.setBackupDocumentAndMethod(testUri)
        val settings = repository.settingsFlow.first()

        assertEquals(testUri, settings.backupDocument)
        assertEquals(Settings.IO_METHOD_DOCUMENT_PROVIDER, settings.backupMethod)
        assertEquals(testUri, settings.displayBackupPath)
    }

    @Test
    fun testUpdateExportDocumentAndMethod() = testScope.runTest {
        val (repository, _) = createRepository()
        val testUri = "content://com.android.providers.documents/document/456"

        repository.setExportDocumentAndMethod(testUri)
        val settings = repository.settingsFlow.first()

        assertEquals(testUri, settings.exportDocument)
        assertEquals(Settings.IO_METHOD_DOCUMENT_PROVIDER, settings.exportMethod)
        assertEquals(testUri, settings.displayExportPath)
    }

    @Test
    fun testLastBackupJulian() = testScope.runTest {
        val (repository, _) = createRepository()

        repository.setLastBackupJulian(2460000)
        val settings = repository.settingsFlow.first()

        assertEquals(2460000, settings.lastBackupJulian)
    }
}
