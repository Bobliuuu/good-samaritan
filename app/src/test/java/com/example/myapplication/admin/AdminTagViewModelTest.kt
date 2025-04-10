package com.example.myapplication.admin

import android.os.Looper
import com.example.myapplication.admin.admintag.AdminTagViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class AdminTagViewModelTest {

    @Before
    fun setUp() {
        stopKoin()
    }

    @Test
    fun testLoadTags() {
        // Create fake DocumentSnapshots for two tags.
        val document1 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(document1.getString("name")).thenReturn("Tag1")
        val document2 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(document2.getString("name")).thenReturn("Tag2")

        // Create a fake QuerySnapshot returning these documents.
        val fakeQuerySnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(fakeQuerySnapshot.documents).thenReturn(listOf(document1, document2))

        // Create a fake CollectionReference that returns our QuerySnapshot.
        val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeCollectionRef.get()).thenReturn(Tasks.forResult(fakeQuerySnapshot))

        // Create a fake FirebaseFirestore that returns the fake collection for "tags".
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("tags")).thenReturn(fakeCollectionRef)

        // Instantiate the real AdminTagViewModel with the fake Firestore.
        val viewModel = AdminTagViewModel(fakeFirestore)

        // Wait a bit for the asynchronous loadTags() callback.
        Thread.sleep(100)
        // Process any queued runnables on the main looper.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the view model's tags list is updated.
        assertEquals(listOf("Tag1", "Tag2"), viewModel.tags.value)
    }

    @Test
    fun testAddTag() {
        // Initial snapshot: empty list.
        val initialSnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(initialSnapshot.documents).thenReturn(emptyList())

        // Updated snapshot: contains the new tag "NewTag".
        val newDocument = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(newDocument.getString("name")).thenReturn("NewTag")
        val updatedSnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(updatedSnapshot.documents).thenReturn(listOf(newDocument))

        // Fake CollectionReference:
        // First call to get() returns the initial empty snapshot,
        // then subsequent call returns the updated snapshot.
        val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeCollectionRef.get())
            .thenReturn(Tasks.forResult(initialSnapshot))
            .thenReturn(Tasks.forResult(updatedSnapshot))

        // Fake add() call returns a successful DocumentReference.
        Mockito.`when`(fakeCollectionRef.add(any<Map<String, String>>()))
            .thenReturn(Tasks.forResult(Mockito.mock(DocumentReference::class.java)))

        // Fake FirebaseFirestore that returns the fake collection for "tags".
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("tags")).thenReturn(fakeCollectionRef)

        // Instantiate the view model.
        val viewModel = AdminTagViewModel(fakeFirestore)
        Thread.sleep(100) // Wait for initial load.
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.tags.value.isEmpty())

        // Call addTag to add "NewTag".
        viewModel.addTag("NewTag")
        Thread.sleep(100) // Wait for the update after adding.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the tags list now contains "NewTag".
        assertEquals(listOf("NewTag"), viewModel.tags.value)
    }

    @Test
    fun testDeleteTag() {
        // Create a fake DocumentSnapshot representing the tag to delete.
        val documentToDelete = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(documentToDelete.getString("name")).thenReturn("TagToDelete")

        // Initial snapshot: contains "TagToDelete".
        val initialSnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(initialSnapshot.documents).thenReturn(listOf(documentToDelete))

        // Updated snapshot: after deletion, returns an empty list.
        val updatedSnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(updatedSnapshot.documents).thenReturn(emptyList())

        // Fake CollectionReference for loadTags():
        // First call returns the initial snapshot, then returns the updated snapshot.
        val fakeCollectionRef = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeCollectionRef.get())
            .thenReturn(Tasks.forResult(initialSnapshot))
            .thenReturn(Tasks.forResult(updatedSnapshot))

        // For deleteTag, simulate a query that returns the document to delete.
        val fakeQuery = Mockito.mock(Query::class.java)
        Mockito.`when`(fakeCollectionRef.whereEqualTo("name", "TagToDelete")).thenReturn(fakeQuery)
        Mockito.`when`(fakeQuery.get()).thenReturn(Tasks.forResult(initialSnapshot))

        // Fake WriteBatch to simulate deletion.
        val fakeBatch = Mockito.mock(WriteBatch::class.java)
        Mockito.`when`(fakeBatch.delete(any())).thenReturn(fakeBatch)
        Mockito.`when`(fakeBatch.commit()).thenReturn(Tasks.forResult(null))

        // Fake FirebaseFirestore that returns the fake collection and batch.
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("tags")).thenReturn(fakeCollectionRef)
        Mockito.`when`(fakeFirestore.batch()).thenReturn(fakeBatch)

        // Instantiate the view model.
        val viewModel = AdminTagViewModel(fakeFirestore)
        Thread.sleep(100) // Wait for initial load.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the tags list initially contains "TagToDelete".
        assertEquals(listOf("TagToDelete"), viewModel.tags.value)

        // Call deleteTag to remove "TagToDelete".
        viewModel.deleteTag("TagToDelete")
        Thread.sleep(100) // Wait for deletion update.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the tags list is now empty.
        assertTrue(viewModel.tags.value.isEmpty())
    }
}
