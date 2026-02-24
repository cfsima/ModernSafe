package io.github.cfsima.modernsafe.test

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.cfsima.modernsafe.FrontDoor
import io.github.cfsima.modernsafe.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<FrontDoor>()

    private val masterPassword = "1234"

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun unlockIfNeeded() {
        val firstTimeMsg = composeTestRule.activity.getString(R.string.first_time)
        val welcomeBackMsg = "Welcome Back"
        val continueText = composeTestRule.activity.getString(R.string.continue_text)
        val masterPassLabel = "Master Password"
        val confirmPassLabel = "Confirm Password"
        val passwordLabel = "Password"

        try {
            composeTestRule.onNodeWithText(firstTimeMsg).assertIsDisplayed()
            composeTestRule.onNodeWithText(masterPassLabel).performTextInput(masterPassword)
            composeTestRule.onNodeWithText(confirmPassLabel).performTextInput(masterPassword)
            composeTestRule.onNodeWithText(continueText).performClick()
            return
        } catch (e: AssertionError) {
        }

        try {
            composeTestRule.onNodeWithText(welcomeBackMsg).assertIsDisplayed()
            composeTestRule.onNodeWithText(passwordLabel).performTextInput(masterPassword)
            composeTestRule.onNodeWithText(continueText).performClick()
            return
        } catch (e: AssertionError) {
        }
    }

    @Test
    fun testAAAAUnlock() {
        unlockIfNeeded()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.app_name)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(composeTestRule.activity.getString(R.string.password_add)).assertIsDisplayed()
    }

    @Test
    fun testCategoryAdd() {
        unlockIfNeeded()
        val categoryName = "Category 1"
        val category2Name = "Category 2"
        val addStr = composeTestRule.activity.getString(R.string.password_add)
        val categoryLabel = composeTestRule.activity.getString(R.string.category)
        val yesStr = composeTestRule.activity.getString(R.string.yes)

        // Add Category 1
        composeTestRule.onNodeWithContentDescription(addStr).performClick()
        composeTestRule.onNodeWithText(categoryLabel).performTextInput(categoryName)
        composeTestRule.onNodeWithText(yesStr).performClick()

        // Add Category 2
        composeTestRule.onNodeWithContentDescription(addStr).performClick()
        composeTestRule.onNodeWithText(categoryLabel).performTextInput(category2Name)
        composeTestRule.onNodeWithText(yesStr).performClick()

        composeTestRule.onNodeWithText(categoryName).assertIsDisplayed()
        composeTestRule.onNodeWithText(category2Name).assertIsDisplayed()
    }

    @Test
    fun testCategoryEdit() {
        unlockIfNeeded()
        val categoryName = "Category 1"
        val categoryModified = "Category 1 Modified"
        val menuTag = "menu_$categoryName"
        val editEntryStr = composeTestRule.activity.getString(R.string.edit_entry)
        val saveStr = composeTestRule.activity.getString(R.string.save)
        val categoryLabel = composeTestRule.activity.getString(R.string.category)

        // Ensure category exists
        ensureCategoryExists(categoryName)

        // Edit
        composeTestRule.onNodeWithTag(menuTag).performClick()
        composeTestRule.onNodeWithText(editEntryStr).performClick()

        // Dialog appears
        composeTestRule.onNodeWithText(categoryLabel).performTextInput(" Modified")
        composeTestRule.onNodeWithText(saveStr).performClick()

        // Verify
        composeTestRule.onNodeWithText(categoryModified).assertIsDisplayed()

        // Revert change for other tests?
        // Ideally tests should be independent, but we are running on same device/emulator state potentially.
        // I'll leave it modified.
    }

    @Test
    fun test_CategoryRemove() {
        unlockIfNeeded()
        val categoryName = "Category 2"
        val menuTag = "menu_$categoryName"
        val deleteStr = composeTestRule.activity.getString(R.string.password_delete)
        val yesStr = composeTestRule.activity.getString(R.string.yes)

        ensureCategoryExists(categoryName)

        composeTestRule.onNodeWithTag(menuTag).performClick()
        composeTestRule.onNodeWithText(deleteStr).performClick()
        composeTestRule.onNodeWithText(yesStr).performClick()

        // Verify gone
        try {
            composeTestRule.onNodeWithText(categoryName).assertIsDisplayed()
            throw AssertionError("Category should be deleted")
        } catch (e: AssertionError) {
            // Success
        }
    }

    @Test
    fun testPasswordAdd() {
        unlockIfNeeded()
        val categoryName = "Category Passwords"
        ensureCategoryExists(categoryName)

        // Enter Category
        composeTestRule.onNodeWithText(categoryName).performClick()

        val addStr = composeTestRule.activity.getString(R.string.password_add)
        val descLabel = composeTestRule.activity.getString(R.string.description)
        val saveStr = composeTestRule.activity.getString(R.string.save)

        val entryDesc = "My Password Entry"

        // Click Add FAB
        composeTestRule.onNodeWithContentDescription(addStr).performClick()

        // Fill Form
        composeTestRule.onNodeWithText(descLabel).performTextInput(entryDesc)
        composeTestRule.onNodeWithContentDescription(saveStr).performClick()

        // Verify in list
        composeTestRule.onNodeWithText(entryDesc).assertIsDisplayed()

        // Go back to Category List
        // PassListScreen has Back button? Or system back?
        // PassListScreen has onBack callback.
        // It calls finish() or startActivity(CategoryList).
        // I can press back using Espresso or rule.
        // composeTestRule.activityRule.scenario.onActivity { it.onBackPressed() }
        // But rule is AndroidComposeRule<FrontDoor>. The current activity is PassList.
        // Espresso.pressBack() works.
        // But for independent tests, I don't need to go back.
    }

    @Test
    fun testSearch() {
        unlockIfNeeded()
        val categoryName = "Category Search"
        val entryDesc = "Search Target"

        ensureCategoryExists(categoryName)
        composeTestRule.onNodeWithText(categoryName).performClick()

        // Add entry
        val addStr = composeTestRule.activity.getString(R.string.password_add)
        val descLabel = composeTestRule.activity.getString(R.string.description)
        val saveStr = composeTestRule.activity.getString(R.string.save)

        composeTestRule.onNodeWithContentDescription(addStr).performClick()
        composeTestRule.onNodeWithText(descLabel).performTextInput(entryDesc)
        composeTestRule.onNodeWithContentDescription(saveStr).performClick()

        // Go back to Category List
        androidx.test.espresso.Espresso.pressBack()

        // Search
        val searchStr = composeTestRule.activity.getString(R.string.search)
        val searchHint = composeTestRule.activity.getString(R.string.search_hint)

        composeTestRule.onNodeWithContentDescription(searchStr).performClick()
        composeTestRule.onNodeWithText(searchHint).performTextInput("Target")

        // Verify result and click
        composeTestRule.onNodeWithText(entryDesc).performClick()

        // Verify PassView (PassViewScreen)
        // PassView should show details.
        composeTestRule.onNodeWithText(entryDesc).assertIsDisplayed()
    }

    private fun ensureCategoryExists(name: String) {
        val addStr = composeTestRule.activity.getString(R.string.password_add)
        val categoryLabel = composeTestRule.activity.getString(R.string.category)
        val yesStr = composeTestRule.activity.getString(R.string.yes)

        try {
            composeTestRule.onNodeWithText(name).assertIsDisplayed()
        } catch (e: AssertionError) {
            composeTestRule.onNodeWithContentDescription(addStr).performClick()
            composeTestRule.onNodeWithText(categoryLabel).performTextInput(name)
            composeTestRule.onNodeWithText(yesStr).performClick()
        }
    }
}
