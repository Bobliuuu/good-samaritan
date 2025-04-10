package com.example.myapplication.screens.notification

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class NotificationViewModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    val displayName = mutableStateOf("")
    private val _radius = MutableStateFlow(10f)
    open val radius: StateFlow<Float> = _radius

    private val _relevance = MutableStateFlow(5f)
    open val relevance: StateFlow<Float> = _relevance

    private val _selectedTags = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    open val selectedTags: StateFlow<Map<String, Boolean>> = _selectedTags

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    open val availableTags: StateFlow<List<String>> = _availableTags

    private val userId: String? get() = auth.currentUser?.uid

    init {
        fetchPreferences()
    }

    open fun fetchPreferences() {
        val uid = userId ?: return

        db.collection("settings").document(uid).collection("preferences").document("profile")
            .get()
            .addOnSuccessListener { doc ->
                doc.getString("name")?.let { displayName.value = it }
            }

        db.collection("settings").document(uid).collection("preferences").document("filters")
            .get()
            .addOnSuccessListener { doc ->
                doc.getDouble("radius")?.toFloat()?.let { _radius.value = it }
                doc.getDouble("relevance")?.toFloat()?.let { _relevance.value = it }

                val selected = (doc.get("tags") as? List<*> ?: emptyList<Any>())
                    .mapNotNull { it?.toString() }
                    .toSet()

                val updatedTags = _availableTags.value.associateWith { it in selected }
                _selectedTags.value = updatedTags
            }

        db.collection("tags").get()
            .addOnSuccessListener { result ->
                val loadedTags = result.documents.mapNotNull { it.getString("name") }
                _availableTags.value = loadedTags

                val current = _selectedTags.value
                val updated = loadedTags.associateWith { current[it] ?: false }
                _selectedTags.value = updated
            }
    }

    open fun updateName(newName: String) {
        displayName.value = newName
        userId?.let {
            db.collection("settings").document(it)
                .collection("preferences").document("profile")
                .set(mapOf("name" to newName), SetOptions.merge())
        }
    }

    open fun updateRadiusAndRelevance(newRadius: Float, newRelevance: Float) {
        _radius.value = newRadius
        _relevance.value = newRelevance
        userId?.let {
            db.collection("settings").document(it)
                .collection("preferences").document("filters")
                .set(mapOf("radius" to newRadius, "relevance" to newRelevance), SetOptions.merge())
        }
    }

    open fun toggleTag(tag: String) {
        val updated = _selectedTags.value.toMutableMap()
        updated[tag] = !(updated[tag] ?: false)
        _selectedTags.value = updated

        userId?.let {
            db.collection("settings").document(it)
                .collection("preferences").document("filters")
                .set(mapOf("tags" to updated.filterValues { it }.keys.toList()), SetOptions.merge())
        }
    }
}
