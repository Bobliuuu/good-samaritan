package com.example.myapplication.admin

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.navigation.NavHostController
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.admin.adminincident.AdminIncidentScreen
import com.example.myapplication.admin.adminincident.AdminIncidentViewModel
import com.example.myapplication.model.AdminIncident
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeAdminIncidentViewModel : AdminIncidentViewModel(fakeFirestore) {
    companion object {
        // Create a fake Firestore instance that returns a dummy CollectionReference for "incidents"
        private val fakeFirestore: FirebaseFirestore = run {
            val fs = Mockito.mock(FirebaseFirestore::class.java)
            val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
            // When collection("incidents") is called, return our fakeCollectionRef.
            Mockito.`when`(fs.collection("incidents")).thenReturn(fakeCollectionRef)
            // Stub addSnapshotListener so it doesn't crash (we don't use its data).
            Mockito.`when`(fakeCollectionRef.addSnapshotListener(Mockito.any<EventListener<QuerySnapshot>>()))
                .thenReturn(Mockito.mock(ListenerRegistration::class.java))
            fs
        }
    }

    // Provide a fixed list of incidents for testing.
    private val _fakeIncidents = MutableStateFlow<List<AdminIncident>>(
        listOf(
            AdminIncident("id1", "Incident1", 5),
            AdminIncident("id2", "Incident2", -15)
        )
    )
    override val incidents: StateFlow<List<AdminIncident>> = _fakeIncidents

    override fun deleteIncident(id: String) {
        _fakeIncidents.value = _fakeIncidents.value.filterNot { it.id == id }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class AdminIncidentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun adminIncidentScreenDisplaysAllElements() {
        val fakeViewModel = FakeAdminIncidentViewModel()
        // For tests where we don't need to verify navigation, use a mocked NavHostController.
        val fakeNavController = Mockito.mock(NavHostController::class.java)
        composeTestRule.setContent {
            AdminIncidentScreen(
                navController = fakeNavController,
                viewModel = fakeViewModel
            )
        }
        // Verify header text.
        composeTestRule.onNodeWithText("Manage Incidents").assertIsDisplayed()
        // Verify that incident items are displayed.
        composeTestRule.onNodeWithText("Incident1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Incident2").assertIsDisplayed()
        // Verify that there are two "Delete" buttons.
        composeTestRule.onAllNodesWithText("Delete").assertCountEquals(2)
    }

    @Test
    fun testIncidentNavigation() {
        val fakeViewModel = FakeAdminIncidentViewModel()
        // Create a mock NavHostController so we can verify navigation.
        val mockNavController = Mockito.mock(NavHostController::class.java)
        composeTestRule.setContent {
            AdminIncidentScreen(
                navController = mockNavController,
                viewModel = fakeViewModel
            )
        }
        // Simulate clicking on the TextButton that displays "Incident1".
        composeTestRule.onNodeWithText("Incident1").performClick()
        // Verify that the navController navigated with the expected route.
        verify(mockNavController).navigate("incident_detail/id1?isAdmin=true")
    }

    @Test
    fun testDeleteIncident() {
        val fakeViewModel = FakeAdminIncidentViewModel()
        val fakeNavController = Mockito.mock(NavHostController::class.java)
        composeTestRule.setContent {
            AdminIncidentScreen(
                navController = fakeNavController,
                viewModel = fakeViewModel
            )
        }
        // Initially, both incidents should be displayed.
        composeTestRule.onNodeWithText("Incident1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Incident2").assertIsDisplayed()

        // Click the "Delete" button for the first incident.
        // We assume the first "Delete" button corresponds to "Incident1".
        composeTestRule.onAllNodesWithText("Delete")[0].performClick()
        composeTestRule.waitForIdle()

        // Verify that "Incident1" is removed.
        composeTestRule.onNodeWithText("Incident1").assertDoesNotExist()
        // "Incident2" should still be present.
        composeTestRule.onNodeWithText("Incident2").assertIsDisplayed()
    }
}
