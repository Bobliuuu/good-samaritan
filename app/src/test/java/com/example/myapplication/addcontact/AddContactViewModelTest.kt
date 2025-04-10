package com.example.myapplication.addcontact

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.myapplication.TestApplication2
import com.example.myapplication.screens.addcontact.AddContactViewModel
import com.example.myapplication.ui.screens.contacts.AddContactScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication2::class)
class AddContactViewModelTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        ShadowToast.reset()
    }

    @Test
    fun testUIElementsDisplay() {
        // Verify that all expected UI elements are displayed.
        composeTestRule.setContent {
            AddContactScreen(onContactAdded = {})
        }
        // Header text.
        composeTestRule.onNodeWithText("Add New Contact").assertIsDisplayed()
        // Name text field label.
        composeTestRule.onNodeWithText("Name").assertIsDisplayed()
        // Phone Number text field label.
        composeTestRule.onNodeWithText("Phone Number (XXX-XXX-XXXX)").assertIsDisplayed()
        // Supporting text.
        composeTestRule.onNodeWithText("Format: XXX-XXX-XXXX").assertIsDisplayed()
        // Save Contact button.
        composeTestRule.onNodeWithText("Save Contact").assertIsDisplayed()
    }

    @Test
    fun testEmptyFieldsValidationShowsToast() {
        composeTestRule.setContent {
            AddContactScreen(onContactAdded = {})
        }
        // Leave fields empty and click the Save Contact button.
        composeTestRule.onNodeWithText("Save Contact").performClick()
        composeTestRule.waitForIdle()
        // Verify that the expected Toast message appears.
        val toastText = ShadowToast.getTextOfLatestToast()
        assert(toastText == "Please fill all fields")
    }

    @Test
    fun testInvalidPhoneFormatShowsToast() {
        composeTestRule.setContent {
            AddContactScreen(onContactAdded = {})
        }
        // Enter a valid name but an invalid phone number.
        composeTestRule.onNodeWithText("Name").performTextInput("John Doe")
        composeTestRule.onNodeWithText("Phone Number (XXX-XXX-XXXX)").performTextInput("invalid")
        composeTestRule.onNodeWithText("Save Contact").performClick()
        composeTestRule.waitForIdle()
        // Verify that the correct toast message appears.
        val toastText = ShadowToast.getTextOfLatestToast()
        assert(toastText == "Invalid phone number format! Use XXX-XXX-XXXX")
    }

    @Test
    fun testValidInputCallsAddContactAndOnContactAdded() {
        // Create a mock AddContactViewModel.
        val mockViewModel = Mockito.mock(AddContactViewModel::class.java)
        // Stub the addContact method to immediately call the onComplete lambda.
        whenever(mockViewModel.addContact(anyString(), anyString(), any())).thenAnswer { invocation ->
            val onComplete = invocation.getArgument<() -> Unit>(2)
            onComplete()
        }

        var contactAddedCalled = false
        composeTestRule.setContent {
            AddContactScreen(
                onContactAdded = { contactAddedCalled = true },
                viewModel = mockViewModel
            )
        }
        // Enter valid input.
        composeTestRule.onNodeWithText("Name").performTextInput("John Doe")
        composeTestRule.onNodeWithText("Phone Number (XXX-XXX-XXXX)").performTextInput("123-456-7890")
        composeTestRule.onNodeWithText("Save Contact").performClick()
        composeTestRule.waitForIdle()

        // Verify that the onContactAdded callback was triggered.
        assert(contactAddedCalled)

        // Verify that a success Toast is shown.
        val toastText = ShadowToast.getTextOfLatestToast()
        assert(toastText == "Contact added successfully!")
    }

}
