package com.example.myapplication.incidentdetail

import com.example.myapplication.TestFirebaseApplication
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.myapplication.model.IncidentMeta
import com.example.myapplication.screens.incidentdetail.IncidentDetailScreen
import com.example.myapplication.screens.incidentdetail.IncidentDetailViewModel
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeIncidentDetailViewModel : IncidentDetailViewModel(
    db = FirebaseFirestore.getInstance(), // Not used in fake
    incidentId = "fakeIncidentId"
) {
    private val _fakeIncidentMeta = MutableStateFlow(
        IncidentMeta(
            id = "fakeIncidentId",
            name = "Test Incident",
            severity = "High",
            description = "This is a test incident",
            tags = listOf("TagA", "TagB"),
            location = "Test City"
        )
    )
    override val incidentMeta: StateFlow<IncidentMeta> get() = _fakeIncidentMeta.asStateFlow()

    private val _fakeMediaUrls = MutableStateFlow(listOf(""))
    override val mediaUrls: StateFlow<List<String>> get() = _fakeMediaUrls.asStateFlow()

    private val _fakeVoteCount = MutableStateFlow(10)
    override val voteCount: StateFlow<Int> get() = _fakeVoteCount.asStateFlow()

    private val _fakeUserVoteValue = MutableStateFlow<Int?>(null)
    override val userVoteValue: MutableStateFlow<Int?> get() = _fakeUserVoteValue

    override fun loadVoteData(userId: String) {
        // No-op for testing.
    }

    override fun voteOnIncident(userId: String, voteValue: Int, onComplete: (Boolean) -> Unit) {
        // For testing, simply invoke onComplete(true).
        onComplete(true)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class IncidentDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun incidentDetailScreenDisplaysAllElements() {
        val fakeViewModel = FakeIncidentDetailViewModel()
        // For UI tests we don't need to verify navigation here.
        composeTestRule.setContent {
            IncidentDetailScreen(
                viewModel = fakeViewModel,
                onBackPressed = {},
                onImageClick = {}
            )
        }
        // Verify that the top app bar title is displayed.
        composeTestRule.onNodeWithText("Incident Details").assertIsDisplayed()
        // Verify that the IncidentCard displays the incident name.
        composeTestRule.onNodeWithTag("IncidentCard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Incident").assertIsDisplayed()
        // Verify that the media carousel displays a media URL.
        composeTestRule.onNodeWithTag("MediaCarousel").assertIsDisplayed()
        // Verify that the vote count is displayed.
        composeTestRule.onNodeWithText("Votes: 10").performScrollTo().assertIsDisplayed()
        // Verify that Upvote and Downvote buttons are displayed.
        composeTestRule.onNodeWithText("Upvote").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Downvote").assertIsDisplayed()
    }

    @Test
    fun testBackButtonNavigation() {
        val fakeViewModel = FakeIncidentDetailViewModel()
        var backPressedCalled = false
        composeTestRule.setContent {
            IncidentDetailScreen(
                viewModel = fakeViewModel,
                onBackPressed = { backPressedCalled = true },
                onImageClick = {}
            )
        }
        // Simulate clicking the back button by targeting the IconButton with content description "Back".
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        // Verify that onBackPressed callback was invoked.
        assertTrue(backPressedCalled)
    }
}
