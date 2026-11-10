package io.github.cfsima.modernsafe

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassEditViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PassEditViewModel {
        val dummyApp = object : Application() {
            override fun getPackageName(): String = "io.github.cfsima.modernsafe"
            override fun getApplicationContext(): Context = this
        }
        return PassEditViewModel(dummyApp)
    }

    @Test
    fun testSubsequentLoadDoesNotEraseEnteredData() = testScope.runTest {
        val viewModel = createViewModel()

        // Initial load for a new entry
        viewModel.load(id = -1L, categoryId = 1L)

        // User enters data in the post/entry
        viewModel.onDescriptionChange("My Bank")
        viewModel.onUsernameChange("carl")
        viewModel.onWebsiteChange("https://bank.example.com")
        viewModel.onNoteChange("Confidential note")

        assertEquals("My Bank", viewModel.uiState.value.description)
        assertEquals("carl", viewModel.uiState.value.username)
        assertEquals("https://bank.example.com", viewModel.uiState.value.website)
        assertEquals("Confidential note", viewModel.uiState.value.note)

        // Simulating navigating to password generator and returning to the edit screen
        // In Compose, returning re-triggers LaunchedEffect which calls load(id, categoryId)
        viewModel.load(id = -1L, categoryId = 1L)

        // Verify entered data was NOT erased
        assertEquals("My Bank", viewModel.uiState.value.description)
        assertEquals("carl", viewModel.uiState.value.username)
        assertEquals("https://bank.example.com", viewModel.uiState.value.website)
        assertEquals("Confidential note", viewModel.uiState.value.note)

        // Copy generated password to current entry
        viewModel.onPasswordChange("genPass#99!")

        assertEquals("genPass#99!", viewModel.uiState.value.password)
        assertEquals("My Bank", viewModel.uiState.value.description)
        assertEquals("carl", viewModel.uiState.value.username)
        assertEquals("https://bank.example.com", viewModel.uiState.value.website)
        assertEquals("Confidential note", viewModel.uiState.value.note)
        assertTrue(viewModel.uiState.value.isModified)
    }

    @Test
    fun testLoadDifferentCategoryOrIdLoadsNewState() = testScope.runTest {
        val viewModel = createViewModel()

        viewModel.load(id = -1L, categoryId = 1L)
        viewModel.onDescriptionChange("Initial Entry")
        assertEquals("Initial Entry", viewModel.uiState.value.description)

        // Loading for a different category should reset state for new entry
        viewModel.load(id = -1L, categoryId = 2L)
        assertEquals("", viewModel.uiState.value.description)
        assertEquals(2L, viewModel.uiState.value.categoryId)
    }
}
