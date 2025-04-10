package com.example.myapplication.incidentdetail

import android.os.Looper
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.model.IncidentMeta
import com.example.myapplication.screens.incidentdetail.IncidentDetailViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import org.junit.Before
import org.koin.core.context.stopKoin
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class IncidentDetailViewModelTest {

    @Before
    fun setUp() {
        // Ensure any previous Koin instance is stopped.
        stopKoin()
    }

    @Test
    fun testLoadIncident() {
        // Create a fake DocumentSnapshot for the incident.
        val fakeDoc = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(fakeDoc.getString("name")).thenReturn("Test Incident")
        Mockito.`when`(fakeDoc.get("severity")).thenReturn("High")
        Mockito.`when`(fakeDoc.getString("description")).thenReturn("This is a test incident")
        Mockito.`when`(fakeDoc.get("images")).thenReturn(listOf("url1", "url2"))
        Mockito.`when`(fakeDoc.get("tags")).thenReturn(listOf("TagA", "TagB"))
        Mockito.`when`(fakeDoc.getLong("votes")).thenReturn(10L)
        Mockito.`when`(fakeDoc.getString("location")).thenReturn("Test City")

        // Create a fake DocumentReference that returns the fakeDoc.
        val fakeDocRef = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(fakeDocRef.get()).thenReturn(Tasks.forResult(fakeDoc))

        // Create a fake CollectionReference for "incidents".
        val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeCollectionRef.document("fakeIncidentId")).thenReturn(fakeDocRef)

        // Create a fake FirebaseFirestore that returns the fake collection.
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("incidents")).thenReturn(fakeCollectionRef)

        // Instantiate the view model.
        val viewModel = IncidentDetailViewModel(fakeFirestore, "fakeIncidentId")
        // Wait for asynchronous callback to finish.
        Thread.sleep(100)
        shadowOf(Looper.getMainLooper()).idle()

        // Verify that the incident meta was updated correctly.
        val meta: IncidentMeta = viewModel.incidentMeta.value
        assertEquals("fakeIncidentId", meta.id)
        assertEquals("Test Incident", meta.name)
        assertEquals("High", meta.severity)
        assertEquals("This is a test incident", meta.description)
        // Also verify that mediaUrls and voteCount were updated.
        assertEquals(listOf("url1", "url2"), viewModel.mediaUrls.value)
        assertEquals(10, viewModel.voteCount.value)
    }

    @Test
    fun testVoteOnIncident() {
        // Create a fake Firestore.
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        // Create a fake CollectionReference for incidents.
        val incidentsCollection = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeFirestore.collection("incidents")).thenReturn(incidentsCollection)
        // Create a fake DocumentReference for our incident.
        val incidentDocRef = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(incidentsCollection.document("fakeIncidentId")).thenReturn(incidentDocRef)

        // For loadVoteData: simulate that incidentDocRef.get() returns a document with 10 votes.
        val fakeIncidentDoc = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(fakeIncidentDoc.getLong("votes")).thenReturn(10L)
        Mockito.`when`(incidentDocRef.get()).thenReturn(Tasks.forResult(fakeIncidentDoc))

        // Stub the votes subcollection on the incident document.
        val votesCollection = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(incidentDocRef.collection("votes")).thenReturn(votesCollection)
        // For user "user1", simulate that no vote document exists.
        val fakeVoteDoc = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(fakeVoteDoc.exists()).thenReturn(false)
        val voteDocRef = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(votesCollection.document("user1")).thenReturn(voteDocRef)
        Mockito.`when`(voteDocRef.get()).thenReturn(Tasks.forResult(fakeVoteDoc))

        // Stub runTransaction to simulate a successful transaction.
        Mockito.`when`(
            fakeFirestore.runTransaction<Void>(
                Mockito.any<com.google.firebase.firestore.Transaction.Function<Void>>()
            )
        ).thenReturn(Tasks.forResult(null as Void?))

        // Instantiate the view model.
        val viewModel = IncidentDetailViewModel(fakeFirestore, "fakeIncidentId")
        var onCompleteResult: Boolean? = null
        viewModel.voteOnIncident("user1", 1) { success ->
            onCompleteResult = success
        }
        Thread.sleep(100)
        shadowOf(Looper.getMainLooper()).idle()

        // Verify that onComplete is called with true.
        assertEquals(true, onCompleteResult)
    }
}
