package com.example.myapplication.map

import android.content.Context
import android.os.Looper
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.screens.map.MapViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class MapViewModelTest {

    private lateinit var viewModel: MapViewModel
    private lateinit var context: Context
    private lateinit var fakeGoogleMap: GoogleMap
    private lateinit var fakeNavController: NavController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Use deep stubs to get a non-null stub for uiSettings.
        fakeGoogleMap = Mockito.mock(GoogleMap::class.java, Mockito.RETURNS_DEEP_STUBS)
        // No need to manually stub getUiSettings() now.

        fakeNavController = Mockito.mock(NavController::class.java)
        viewModel = MapViewModel()
    }


    @Test
    fun testOnMapReadySetsMapReady() {
        // Call onMapReady with our fake GoogleMap, fake NavController, context, a radius, and selected tags.
        viewModel.onMapReady(
            map = fakeGoogleMap,
            navController = fakeNavController,
            context = context,
            radiusKm = 5f,
            selectedTags = setOf("Tag1", "Tag2")
        )
        // Wait a bit for asynchronous callbacks.
        Thread.sleep(100)
        shadowOf(Looper.getMainLooper()).idle()
        // Assert that isMapReady is true.
        assertTrue(viewModel.isMapReady.value)
    }
}
