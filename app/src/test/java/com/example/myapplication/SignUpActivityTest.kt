package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignUpActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockAuth: FirebaseAuth

    @Before
    fun setUp() {
        stopKoin()
        mockAuth = mockk(relaxed = true)
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockAuth
    }

    @Test
    fun signUpScreen_DisplaysAllComponents() {
        composeTestRule.setContent {
            SignUpScreen(onSignUpSuccess = {})
        }

        val signUpNodes = composeTestRule.onAllNodesWithText("Sign Up")
        signUpNodes.assertCountEquals(2)

        signUpNodes[0].assert(!hasClickAction())
        signUpNodes[1].assertHasClickAction()

        composeTestRule.onNodeWithText("Email")
            .assertIsDisplayed()
            .assert(hasSetTextAction())
        composeTestRule.onNodeWithText("Password")
            .assertIsDisplayed()
            .assert(hasSetTextAction())

        composeTestRule.onNodeWithText("Back")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun signUpScreen_ShowsErrorMessageWhenFieldsEmpty() {
        composeTestRule.setContent {
            SignUpScreen(onSignUpSuccess = {})
        }

        val signUpNodes = composeTestRule.onAllNodesWithText("Sign Up")
        signUpNodes.assertCountEquals(2)
        val signUpButton = signUpNodes[1]
        signUpButton.performClick()
        composeTestRule.onNodeWithText("Please enter credentials").assertIsDisplayed()
    }

    @Test
    fun signUpScreen_ShowsPasswordLengthError() {
        composeTestRule.setContent {
            SignUpScreen(onSignUpSuccess = {})
        }

        composeTestRule.onNodeWithText("Email").performTextInput("test@email.com")
        composeTestRule.onNodeWithText("Password").performTextInput("123")

        val signUpNodes = composeTestRule.onAllNodesWithText("Sign Up")
        val signUpButton = signUpNodes[1]
        signUpButton.performClick()

        composeTestRule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun signUpScreen_SuccessfulSignUpCallsOnSignUpSuccess() {
        val onSignUpSuccess = mockk<() -> Unit>(relaxed = true)
        val mockTask = mockk<com.google.android.gms.tasks.Task<AuthResult>>(relaxed = true)

        every { mockTask.isSuccessful } returns true
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns mockTask
        every { mockTask.addOnCompleteListener(any()) } answers {
            // Immediately call the listener with the mocked task
            firstArg<OnCompleteListener<AuthResult>>().onComplete(mockTask)
            mockTask
        }


        composeTestRule.setContent {
            SignUpScreen(onSignUpSuccess = onSignUpSuccess)
        }

        composeTestRule.onNodeWithText("Email").performTextInput("test@email.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")

        val signUpNodes = composeTestRule.onAllNodesWithText("Sign Up")
        val signUpButton = signUpNodes[1]
        signUpButton.performClick()

        verify { onSignUpSuccess() }
    }
}
