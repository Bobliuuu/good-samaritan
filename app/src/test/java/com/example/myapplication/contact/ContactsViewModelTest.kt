package com.example.myapplication.contact

import android.os.Looper
import com.example.contactsapp.model.Contact
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.navigation.ContactsViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.koin.core.context.stopKoin
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class ContactsViewModelTest {

    @Before
    fun setUp() {
        stopKoin()
    }

    @Test
    fun testFetchContacts() {
        // Create fake DocumentSnapshots for two contacts.
        val doc1 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(doc1.getString("name")).thenReturn("Alice")
        Mockito.`when`(doc1.getString("number")).thenReturn("1234567890")

        val doc2 = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(doc2.getString("name")).thenReturn("Bob")
        Mockito.`when`(doc2.getString("number")).thenReturn("0987654321")

        // Create a fake QuerySnapshot returning these documents.
        val fakeQuerySnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(fakeQuerySnapshot.documents).thenReturn(listOf(doc1, doc2))

        // Fake the collection for "numbers": db.collection("contacts").document(userId).collection("numbers")
        val fakeNumbersCollection = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeNumbersCollection.get()).thenReturn(Tasks.forResult(fakeQuerySnapshot))

        // Fake the document reference for the user.
        val fakeDocumentReference = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(fakeDocumentReference.collection("numbers")).thenReturn(fakeNumbersCollection)

        // Fake the "contacts" collection.
        val fakeContactsCollection = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeContactsCollection.document("user1")).thenReturn(fakeDocumentReference)

        // Fake FirebaseFirestore that returns the fake contacts collection.
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("contacts")).thenReturn(fakeContactsCollection)

        // Fake FirebaseAuth: simulate a logged in user with uid "user1".
        val fakeUser = Mockito.mock(FirebaseUser::class.java)
        Mockito.`when`(fakeUser.uid).thenReturn("user1")
        val fakeAuth = Mockito.mock(FirebaseAuth::class.java)
        Mockito.`when`(fakeAuth.currentUser).thenReturn(fakeUser)

        // Instantiate ContactsViewModel.
        val viewModel = ContactsViewModel(fakeFirestore, fakeAuth)
        viewModel.fetchContacts()

        // Wait for asynchronous callbacks.
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the contacts list is updated.
        val expectedContacts = listOf(
            Contact("Alice", "1234567890"),
            Contact("Bob", "0987654321")
        )
        assertEquals(expectedContacts, viewModel.contacts.value)
    }

    @Test
    fun testDeleteContact() {
        // Prepare a Contact to delete.
        val contactToDelete = Contact("Alice", "1234567890")

        // Create a fake DocumentSnapshot representing the contact to delete.
        val docToDelete = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(docToDelete.getString("name")).thenReturn("Alice")
        Mockito.`when`(docToDelete.getString("number")).thenReturn("1234567890")

        // Fake a QuerySnapshot for deletion: returns one document.
        val deletionSnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(deletionSnapshot.documents).thenReturn(listOf(docToDelete))

        // Fake the collection for "numbers".
        val fakeNumbersCollection = Mockito.mock(CollectionReference::class.java)
        // Simulate query for deletion:
        val fakeQuery = Mockito.mock(Query::class.java)
        Mockito.`when`(fakeNumbersCollection.whereEqualTo("name", "Alice")).thenReturn(fakeQuery)
        Mockito.`when`(fakeQuery.whereEqualTo("number", "1234567890")).thenReturn(fakeQuery)
        Mockito.`when`(fakeQuery.get()).thenReturn(Tasks.forResult(deletionSnapshot))

        // Fake deletion: document.reference.delete() returns a successful task.
        val fakeDocRef = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(docToDelete.reference).thenReturn(fakeDocRef)
        Mockito.`when`(fakeDocRef.delete()).thenReturn(Tasks.forResult(null))

        // For fetchContacts after deletion, simulate an empty QuerySnapshot.
        val emptySnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(emptySnapshot.documents).thenReturn(emptyList())
        // Setup fakeNumbersCollection.get() to return deletion snapshot first, then empty snapshot.
        Mockito.`when`(fakeNumbersCollection.get())
            .thenReturn(Tasks.forResult(deletionSnapshot))
            .thenReturn(Tasks.forResult(emptySnapshot))

        // Fake chain for Firestore:
        val fakeDocumentReference = Mockito.mock(DocumentReference::class.java)
        Mockito.`when`(fakeDocumentReference.collection("numbers")).thenReturn(fakeNumbersCollection)
        val fakeContactsCollection = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(fakeContactsCollection.document("user1")).thenReturn(fakeDocumentReference)
        val fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(fakeFirestore.collection("contacts")).thenReturn(fakeContactsCollection)

        // Fake FirebaseAuth with uid "user1".
        val fakeUser = Mockito.mock(FirebaseUser::class.java)
        Mockito.`when`(fakeUser.uid).thenReturn("user1")
        val fakeAuth = Mockito.mock(FirebaseAuth::class.java)
        Mockito.`when`(fakeAuth.currentUser).thenReturn(fakeUser)

        // Instantiate ContactsViewModel.
        val viewModel = ContactsViewModel(fakeFirestore, fakeAuth)
        viewModel.fetchContacts()
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that contacts initially contains the contact to delete.
        assertEquals(listOf(contactToDelete), viewModel.contacts.value)

        // Call deleteContact.
        var onCompleteCalled = false
        viewModel.deleteContact(contactToDelete) {
            onCompleteCalled = true
        }
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify that the contacts list is now empty and onComplete is called.
        assertTrue(viewModel.contacts.value.isEmpty())
        assertTrue(onCompleteCalled)
    }
}
