package com.example.myapplication.addincident

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.screens.addincident.AddIncidentViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.stopKoin
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class AddIncidentViewModelTest {

    private lateinit var fakeFirestore: FirebaseFirestore
    private lateinit var fakeStorage: FirebaseStorage
    private lateinit var fakeAuth: FirebaseAuth

    @Before
    fun setUp() {
        stopKoin()

        fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        fakeStorage = Mockito.mock(FirebaseStorage::class.java)
        fakeAuth = Mockito.mock(FirebaseAuth::class.java)
    }

    @Test
    fun testLoadTags() {
        // Set up static mocking so that FirebaseFirestore.getInstance() returns our fakeFirestore.
        val firestoreStatic = mockStatic(FirebaseFirestore::class.java)
        firestoreStatic.`when`<FirebaseFirestore> { FirebaseFirestore.getInstance() }
            .thenReturn(fakeFirestore)

        // Create fake DocumentSnapshots for two tags.
        val fakeDoc1 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(fakeDoc1.getString("name")).thenReturn("Tag1")
        val fakeDoc2 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(fakeDoc2.getString("name")).thenReturn("Tag2")

        // Create a fake QuerySnapshot returning these documents.
        val fakeQuerySnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(fakeQuerySnapshot.documents).thenReturn(listOf(fakeDoc1, fakeDoc2))

        // Create a fake CollectionReference that returns our QuerySnapshot.
        val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeCollectionRef.get()).thenReturn(Tasks.forResult(fakeQuerySnapshot))

        // Stub the collection("tags") call on our fakeFirestore.
        Mockito.`when`(fakeFirestore.collection("tags")).thenReturn(fakeCollectionRef)

        // Instantiate the view model.
        val viewModel = AddIncidentViewModel(fakeFirestore, fakeStorage, fakeAuth)

        // Wait a bit for the asynchronous loadTags() callback.
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the view model's tags list is updated.
        assertEquals(listOf("Tag1", "Tag2"), viewModel.tags.value)

        firestoreStatic.close()
    }

    @Test
    fun testUploadIncidentNoImages() {
        // Simulate a logged-in user.
        val fakeUser = Mockito.mock(FirebaseUser::class.java)
        Mockito.`when`(fakeUser.uid).thenReturn("user1")
        Mockito.`when`(fakeAuth.currentUser).thenReturn(fakeUser)

        // Fake the incidents collection.
        val fakeIncidentsCollection = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeIncidentsCollection.add(any<Map<String, Any>>()))
            .thenReturn(Tasks.forResult(Mockito.mock(DocumentReference::class.java)))

        // Fake settings: simulate a successful set() on the preferences document.
        val fakeSettingsCollection = Mockito.mock(CollectionReference::class.java)
        val fakeFiltersDoc = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(fakeSettingsCollection.document("filters"))
            .thenReturn(fakeFiltersDoc)
        Mockito.`when`(fakeFiltersDoc.set(any<Map<String, Any>>(), any()))
            .thenReturn(Tasks.forResult(null))
        val fakeSettingsDocRef = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(fakeFirestore.collection("settings")).thenReturn(Mockito.mock(CollectionReference::class.java))
        Mockito.`when`(fakeFirestore.collection("settings").document("user1")).thenReturn(fakeSettingsDocRef)
        Mockito.`when`(fakeSettingsDocRef.collection("preferences")).thenReturn(fakeSettingsCollection)

        // Stub the incidents collection call.
        Mockito.`when`(fakeFirestore.collection("incidents")).thenReturn(fakeIncidentsCollection)

        // Instantiate the view model.
        val viewModel = AddIncidentViewModel(fakeFirestore, fakeStorage, fakeAuth)

        // Prepare parameters.
        val context = ApplicationProvider.getApplicationContext<Context>()
        var onCompleteResult: Boolean? = null

        // Call uploadImagesAndSaveIncident with an empty list of image URIs.
        viewModel.uploadImagesAndSaveIncident(
            name = "Test Incident",
            lat = 0.0,
            lon = 0.0,
            severity = 1,
            description = "Test description",
            selectedTags = listOf("Tag1"),
            imageUris = emptyList(),
            context = context
        ) { success ->
            onCompleteResult = success
        }

        // Wait for asynchronous callbacks.
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that onComplete was called with true.
        assertTrue(onCompleteResult == true)
        // And no images were uploaded.
        assertTrue(viewModel.uploadedImageUrls.value.isEmpty())
    }

    @Test
    fun testUploadIncidentUserNotLoggedIn() {
        // Simulate no user logged in.
        Mockito.`when`(fakeAuth.currentUser).thenReturn(null)

        val viewModel = AddIncidentViewModel(fakeFirestore, fakeStorage, fakeAuth)
        val context = ApplicationProvider.getApplicationContext<Context>()
        var onCompleteResult: Boolean? = null

        viewModel.uploadImagesAndSaveIncident(
            name = "Test Incident",
            lat = 0.0,
            lon = 0.0,
            severity = 1,
            description = "Test description",
            selectedTags = listOf("Tag1"),
            imageUris = emptyList(),
            context = context
        ) { success ->
            onCompleteResult = success
        }

        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Expect false because no user is logged in.
        assertTrue(onCompleteResult == false)
    }
}
