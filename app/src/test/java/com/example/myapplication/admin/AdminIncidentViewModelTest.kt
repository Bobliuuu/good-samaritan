package com.example.myapplication.admin

import android.os.Looper
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.admin.adminincident.AdminIncidentViewModel
import com.example.myapplication.model.AdminIncident
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ListenerRegistration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.stopKoin
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class AdminIncidentViewModelTest {

    @Before
    fun setUp() {
        stopKoin()
    }

    @Test
    fun testLoadIncidents() {
        // Create fake DocumentSnapshots for two incidents.
        val document1 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(document1.getString("name")).thenReturn("Incident1")
        Mockito.`when`(document1.getLong("votes")).thenReturn(3L)
        Mockito.`when`(document1.id).thenReturn("id1")

        val document2 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(document2.getString("name")).thenReturn("Incident2")
        Mockito.`when`(document2.getLong("votes")).thenReturn(5L)
        Mockito.`when`(document2.id).thenReturn("id2")

        // Create a fake QuerySnapshot returning these documents.
        val fakeQuerySnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(fakeQuerySnapshot.documents).thenReturn(listOf(document1, document2))

        // Create a fake CollectionReference for "incidents".
        val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
        // When addSnapshotListener is called, immediately invoke the listener callback.
        Mockito.doAnswer { invocation ->
            val listener = invocation.getArgument<EventListener<QuerySnapshot>>(0)
            listener.onEvent(fakeQuerySnapshot, null)
            object : ListenerRegistration {
                override fun remove() {}
            }
        }.`when`(fakeCollectionRef).addSnapshotListener(Mockito.any<EventListener<QuerySnapshot>>())

        // Create a fake FirebaseFirestore that returns the fake collection for "incidents".
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("incidents")).thenReturn(fakeCollectionRef)

        // Instantiate the view model.
        val viewModel = AdminIncidentViewModel(fakeFirestore)

        // Wait for the asynchronous snapshot callback to run.
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the view model's incidents list is updated.
        val expected = listOf(
            AdminIncident("id1", "Incident1", 3),
            AdminIncident("id2", "Incident2", 5)
        )
        assertEquals(expected, viewModel.incidents.value)
    }

    @Test
    fun testDeleteIncident() {
        // Create a fake DocumentSnapshot representing the incident to delete.
        val documentToDelete = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(documentToDelete.getString("name")).thenReturn("Incident1")
        Mockito.`when`(documentToDelete.getLong("votes")).thenReturn(3L)
        Mockito.`when`(documentToDelete.id).thenReturn("id1")

        // Create an initial fake QuerySnapshot that contains the incident.
        val initialSnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(initialSnapshot.documents).thenReturn(listOf(documentToDelete))

        // Create an updated fake QuerySnapshot that returns an empty list (simulating deletion).
        val updatedSnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(updatedSnapshot.documents).thenReturn(emptyList())

        // Create a fake CollectionReference for "incidents" and capture the snapshot listener.
        val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
        var capturedListener: EventListener<QuerySnapshot>? = null
        Mockito.doAnswer { invocation ->
            capturedListener = invocation.getArgument<EventListener<QuerySnapshot>>(0)
            // Immediately invoke with initial snapshot.
            capturedListener?.onEvent(initialSnapshot, null)
            object : ListenerRegistration {
                override fun remove() {}
            }
        }.`when`(fakeCollectionRef).addSnapshotListener(Mockito.any<EventListener<QuerySnapshot>>())

        // Stub the delete call: when delete is called on the document reference, return a successful task.
        val fakeDocumentRef = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(fakeDocumentRef.delete()).thenReturn(Tasks.forResult(null))
        Mockito.`when`(fakeCollectionRef.document("id1")).thenReturn(fakeDocumentRef)

        // Create a fake FirebaseFirestore that returns the fake collection for "incidents".
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("incidents")).thenReturn(fakeCollectionRef)

        // Instantiate the view model.
        val viewModel = AdminIncidentViewModel(fakeFirestore)

        // Wait for initial snapshot.
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the incidents list initially contains the incident.
        assertEquals(listOf(AdminIncident("id1", "Incident1", 3)), viewModel.incidents.value)

        // Call deleteIncident.
        viewModel.deleteIncident("id1")
        // Wait a bit for deletion to occur.
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Now simulate that Firestore sends an updated snapshot with no incidents.
        capturedListener?.onEvent(updatedSnapshot, null)
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the incidents list is now empty.
        assertTrue(viewModel.incidents.value.isEmpty())
    }
}

