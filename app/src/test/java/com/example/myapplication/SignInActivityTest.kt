package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class SignInScreenTest {

    // Use createAndroidComposeRule for SignInActivity so that we can capture navigation.
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SignInActivity>()

    private lateinit var firebaseAuthStatic: MockedStatic<FirebaseAuth>
    private lateinit var firebaseFirestoreStatic: MockedStatic<FirebaseFirestore>
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var fakeFirestore: FirebaseFirestore

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        // Set up static mocks for FirebaseAuth and FirebaseFirestore.
        firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth::class.java)
        mockAuth = Mockito.mock(FirebaseAuth::class.java)
        firebaseAuthStatic.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)

        firebaseFirestoreStatic = Mockito.mockStatic(FirebaseFirestore::class.java)
        fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        firebaseFirestoreStatic.`when`<FirebaseFirestore> { FirebaseFirestore.getInstance() }.thenReturn(fakeFirestore)

        // Reset any previously shown Toasts.
        ShadowToast.reset()
    }

    @After
    fun tearDown() {
        firebaseAuthStatic.close()
        firebaseFirestoreStatic.close()
    }

    @Test
    fun testEmptyFieldsShowsError() {
        // Since SignInActivity already sets its content, we simply query its UI.
        // Leave email and password empty; click sign in.
        composeTestRule.onNodeWithTag("signInButton").performClick()
        composeTestRule.waitForIdle()
        // Verify that error text "Please fill in all fields" appears.
        composeTestRule.onNodeWithText("Please fill in all fields").assertExists()
    }

    @Test
    fun testInvalidEmailShowsError() {
        // Enter an invalid email and a non-empty password.
        composeTestRule.onNodeWithText("Email").performTextInput("invalid-email")
        composeTestRule.onNodeWithText("Password").performTextInput("password")
        composeTestRule.onNodeWithTag("signInButton").performClick()
        composeTestRule.waitForIdle()
        // Verify that error text "Invalid email format" appears.
        composeTestRule.onNodeWithText("Invalid email format").assertExists()
    }

    @Test
    fun testSignInFailureShowsError() {
        // Simulate a failed sign in.
        val exception = Exception("Invalid credentials")
        val failedTask = Tasks.forException<AuthResult>(exception)
        Mockito.`when`(mockAuth.signInWithEmailAndPassword(eq("user@example.com"), eq("password123")))
            .thenReturn(failedTask)

        // Input credentials.
        composeTestRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithTag("signInButton").performClick()
        composeTestRule.waitForIdle()
        // Wait for asynchronous callbacks.
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that error text "Login failed: Invalid credentials" appears.
        composeTestRule.onNodeWithText("Login failed: Invalid credentials").assertExists()
    }

    @Test
    fun testSignInSuccessNavigates() {
        // Simulate a successful sign in.
        val fakeAuthResult = Mockito.mock(AuthResult::class.java)
        val fakeUser = Mockito.mock(FirebaseUser::class.java)
        Mockito.`when`(fakeUser.uid).thenReturn("user123")
        Mockito.`when`(fakeAuthResult.user).thenReturn(fakeUser)
        val successTask = Tasks.forResult(fakeAuthResult)
        Mockito.`when`(mockAuth.signInWithEmailAndPassword(eq("user@example.com"), eq("password123")))
            .thenReturn(successTask)
        Mockito.`when`(mockAuth.currentUser).thenReturn(fakeUser)

        // Simulate Firestore call for user role.
        val fakeDoc = Mockito.mock(com.google.firebase.firestore.DocumentSnapshot::class.java)
        Mockito.`when`(fakeDoc.getBoolean("admin")).thenReturn(false)
        val docTask = Tasks.forResult(fakeDoc)
        val fakeUsersCollection = Mockito.mock(com.google.firebase.firestore.CollectionReference::class.java)
        Mockito.`when`(fakeFirestore.collection("users")).thenReturn(fakeUsersCollection)
        val fakeUserDoc = Mockito.mock(com.google.firebase.firestore.DocumentReference::class.java)
        Mockito.`when`(fakeUsersCollection.document("user123")).thenReturn(fakeUserDoc)
        Mockito.`when`(fakeUserDoc.get()).thenReturn(docTask)

        // Input valid credentials.
        composeTestRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithTag("signInButton").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that no error message is displayed.
        composeTestRule.onNodeWithText("Login failed:").assertDoesNotExist()
        // Capture the activity's started intent.
        val activity: Activity = composeTestRule.activity
        val shadowActivity = shadowOf(activity)
        val startedIntent: Intent? = shadowActivity.nextStartedActivity
        // Check that the intent targets MainActivity.
        assertEquals("com.example.myapplication.MainActivity", startedIntent?.component?.className)
    }
}
