package com.example.myapplication.maptest

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.navigation.NavHostController
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.screens.map.MapScreen
import com.example.myapplication.screens.map.MapViewModel
import com.example.myapplication.screens.notification.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Fake implementation of MapViewModel for testing.
// Production code defines isMapReady as a Compose State<Boolean>.
open class FakeMapViewModel : MapViewModel() {
    // Override isMapReady as a Compose State<Boolean>
    override val isMapReady: State<Boolean> = mutableStateOf(true)
    // Provide an empty marker list.
    override val markerList: List<com.google.android.gms.maps.model.Marker> = emptyList()
    // For testing, mMap is not used.
    override lateinit var mMap: com.google.android.gms.maps.GoogleMap

    // Override refreshFilteredMarkers as no-op for testing.
    override fun refreshFilteredMarkers(radius: Float, selectedTags: Set<String>) {
        // no-op
    }
}

class FakeNotificationViewModel : NotificationViewModel(
    auth = Mockito.mock(FirebaseAuth::class.java),
    db = Mockito.mock(FirebaseFirestore::class.java)
) {
    override val radius = MutableStateFlow(5f)
    override val selectedTags = MutableStateFlow(mapOf("Tag1" to true, "Tag2" to true))
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun mapScreenDisplaysLoadedMap() {
        val fakeMapViewModel = FakeMapViewModel()
        val fakeNavController = Mockito.mock(NavHostController::class.java)
        val fakeNotificationViewModel = FakeNotificationViewModel()

        composeTestRule.setContent {
            MapScreen(
                navController = fakeNavController,
                viewModel = fakeMapViewModel,
                notificationViewModel = fakeNotificationViewModel
            )
        }
        // Verify the top app bar is displayed (using test tag if available or by text).
        composeTestRule.onNode(hasTestTag("TopAppBar")).assertIsDisplayed()
        composeTestRule.onNodeWithText("Map").assertIsDisplayed()
        // Verify that the search overlay is displayed.
        composeTestRule.onNode(hasTestTag("SearchBar")).assertIsDisplayed()
        // Since isMapReady is true, the loading indicator should not be displayed.
        composeTestRule.onNode(hasTestTag("LoadingIndicator")).assertDoesNotExist()
    }

    @Test
    fun mapScreenDisplaysLoadingIndicatorWhenMapNotReady() {
        // Create a fake MapViewModel that reports map not ready.
        val fakeMapViewModel = object : FakeMapViewModel() {
            override val isMapReady: State<Boolean> = mutableStateOf(false)
        }
        val fakeNavController = Mockito.mock(NavHostController::class.java)
        val fakeNotificationViewModel = FakeNotificationViewModel()

        composeTestRule.setContent {
            MapScreen(
                navController = fakeNavController,
                viewModel = fakeMapViewModel,
                notificationViewModel = fakeNotificationViewModel
            )
        }
        // When isMapReady is false, the loading indicator should be displayed.
        composeTestRule.onNode(hasTestTag("LoadingIndicator")).assertIsDisplayed()
    }
}
