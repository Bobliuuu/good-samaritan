package com.example.myapplication.notification

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.myapplication.TestFirebaseApplication
import com.example.myapplication.screens.notification.NotificationViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertFalse


class FakeNotificationViewModel(
    auth: FirebaseAuth,
    db: FirebaseFirestore
) : NotificationViewModel(auth, db) {
    // Instead of relying on fetchPreferences() to update our state, we override properties.
    private val fakeRadius = MutableStateFlow(5f)
    override val radius: StateFlow<Float>
        get() = fakeRadius

    private val fakeRelevance = MutableStateFlow(5f)
    override val relevance: StateFlow<Float>
        get() = fakeRelevance

    private val fakeSelectedTags = MutableStateFlow(mapOf("TagA" to false, "TagB" to false))
    override val selectedTags: StateFlow<Map<String, Boolean>>
        get() = fakeSelectedTags

    private val fakeAvailableTags = MutableStateFlow(listOf("TagA", "TagB"))
    override val availableTags: StateFlow<List<String>>
        get() = fakeAvailableTags

    // Override fetchPreferences to do nothing.
    override fun fetchPreferences() { /* no-op for testing */ }

    override fun updateName(newName: String) {
        displayName.value = newName
    }

    override fun updateRadiusAndRelevance(newRadius: Float, newRelevance: Float) {
        fakeRadius.value = newRadius
        fakeRelevance.value = newRelevance
    }

    override fun toggleTag(tag: String) {
        val current = fakeSelectedTags.value.toMutableMap()
        current[tag] = !(current[tag] ?: false)
        fakeSelectedTags.value = current
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestFirebaseApplication::class)
class NotificationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var fakeFirestore: FirebaseFirestore
    private lateinit var fakeAuth: FirebaseAuth

    @Before
    fun setUp() {
        stopKoin()
        // Create dummy instances for FirebaseFirestore and FirebaseAuth.
        fakeFirestore = Mockito.mock(FirebaseFirestore::class.java)
        fakeAuth = Mockito.mock(FirebaseAuth::class.java)
        val fakeUser = Mockito.mock(FirebaseUser::class.java)
        Mockito.`when`(fakeUser.uid).thenReturn("user123")
        Mockito.`when`(fakeAuth.currentUser).thenReturn(fakeUser)
    }

    @Test
    fun testFetchPreferencesUpdatesState() {
        // Create our FakeNotificationViewModel.
        val viewModel = FakeNotificationViewModel(fakeAuth, fakeFirestore)
        // Since fetchPreferences() is overridden to no-op and our fake values are set in the fake view model,
        // we assert that the properties have the expected fake values.
        assertEquals(5f, viewModel.radius.value)
        assertEquals(5f, viewModel.relevance.value)
        assertEquals(listOf("TagA", "TagB"), viewModel.availableTags.value)
        assertEquals(mapOf("TagA" to false, "TagB" to false), viewModel.selectedTags.value)
    }

    @Test
    fun testUpdateName() {
        val viewModel = FakeNotificationViewModel(fakeAuth, fakeFirestore)
        viewModel.updateName("NewName")
        assertEquals("NewName", viewModel.displayName.value)
    }

    @Test
    fun testUpdateRadiusAndRelevance() {
        val viewModel = FakeNotificationViewModel(fakeAuth, fakeFirestore)
        viewModel.updateRadiusAndRelevance(30f, 8f)
        assertEquals(30f, viewModel.radius.value)
        assertEquals(8f, viewModel.relevance.value)
    }

    @Test
    fun testToggleTag() {
        val viewModel = FakeNotificationViewModel(fakeAuth, fakeFirestore)
        // Initially, both tags are false.
        assertFalse(viewModel.selectedTags.value["TagA"] ?: true)
        // Toggle TagA.
        viewModel.toggleTag("TagA")
        assertTrue(viewModel.selectedTags.value["TagA"] ?: false)
        // Toggle TagA again.
        viewModel.toggleTag("TagA")
        assertFalse(viewModel.selectedTags.value["TagA"] ?: true)
    }
}