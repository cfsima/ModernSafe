@file:Suppress("DestructuringDeclaration", "MoveLambdaOutsideParentheses")

package io.github.cfsima.modernsafe

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class SettingsViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var repoScope: CoroutineScope

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repoScope = CoroutineScope(SupervisorJob() + testDispatcher)
    }

    @After
    fun tearDown() {
        repoScope.cancel()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): Pair<SettingsViewModel, SettingsRepository> {
        val testFile = tmpFolder.newFile("test_vm_${System.nanoTime()}.preferences_pb")
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = repoScope,
            produceFile = { testFile },
        )

        val filesDir = tmpFolder.newFolder("files_vm_${System.nanoTime()}")
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

        val dummyApp = object : Application() {
            override fun getPackageName(): String = "io.github.cfsima.modernsafe"
            override fun getApplicationContext(): Context = dummyContext
            override fun getFilesDir(): File = filesDir
            override fun getExternalFilesDir(type: String?): File = filesDir
        }

        val viewModel = SettingsViewModel(
            application = dummyApp,
            settingsRepository = repository,
        )

        return Pair(viewModel, repository)
    }

    @Test
    fun testInitialValues() = testScope.runTest {
        val (viewModel, _) = createViewModel()

        assertFalse(viewModel.externalAccess.value)
        assertEquals("5", viewModel.lockTimeout.value)
        assertTrue(viewModel.lockOnScreenLock.value)
        assertFalse(viewModel.keypad.value)
        assertFalse(viewModel.keypadMute.value)
        assertTrue(viewModel.autoBackup.value)
        assertEquals("7", viewModel.autoBackupDays.value)
    }

    @Test
    fun testUpdatesReflectedInViewModel() = testScope.runTest {
        val (viewModel, repository) = createViewModel()

        viewModel.setExternalAccess(enabled = true)
        viewModel.setLockTimeout("10")
        viewModel.setLockOnScreenLock(enabled = false)
        viewModel.setKeypad(enabled = true)
        viewModel.setKeypadMute(enabled = true)
        viewModel.setAutoBackup(enabled = false)
        viewModel.setAutoBackupDays("14")

        val settings = repository.settingsFlow.first { it.externalAccess && (it.lockTimeout == "10") }
        assertTrue(settings.externalAccess)
        assertEquals("10", settings.lockTimeout)
        assertFalse(settings.lockOnScreenLock)
        assertTrue(settings.keypad)
        assertTrue(settings.keypadMute)
        assertFalse(settings.autoBackup)
        assertEquals("14", settings.autoBackupDays)
    }

    @Test
    fun testDocumentPathUpdates() = testScope.runTest {
        val (viewModel, repository) = createViewModel()
        val docUri = "content://com.android.providers.documents/document/test_backup"

        viewModel.setBackupDocument(docUri)

        val settings = repository.settingsFlow.first { it.backupDocument == docUri }
        assertEquals(docUri, settings.backupDocument)
        assertEquals(Settings.IO_METHOD_DOCUMENT_PROVIDER, settings.backupMethod)
        assertEquals(docUri, settings.displayBackupPath)
    }

    @Test
    fun testResetSettings() = testScope.runTest {
        val (viewModel, repository) = createViewModel()

        viewModel.setExternalAccess(enabled = true)
        viewModel.setLockTimeout("10")
        viewModel.setKeypad(enabled = true)
        viewModel.setAutoBackup(enabled = false)

        val modified = repository.settingsFlow.first { it.externalAccess && (it.lockTimeout == "10") }
        assertTrue(modified.externalAccess)
        assertEquals("10", modified.lockTimeout)

        viewModel.resetSettings()

        val reset = repository.settingsFlow.first { !it.externalAccess && (it.lockTimeout == "5") }
        assertFalse(reset.externalAccess)
        assertEquals("5", reset.lockTimeout)
        assertTrue(reset.lockOnScreenLock)
        assertFalse(reset.keypad)
        assertTrue(reset.autoBackup)
        assertEquals("7", reset.autoBackupDays)
    }
}
