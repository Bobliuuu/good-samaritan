package com.example.myapplication.admin.admintag

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdminTagViewModel(private val db: FirebaseFirestore) : ViewModel() {
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags

    init {
        loadTags()
    }

    private fun loadTags() {
        db.collection("tags").get().addOnSuccessListener { result ->
            _tags.value = result.documents.mapNotNull { it.getString("name") }
        }
    }

    fun addTag(tag: String) {
        db.collection("tags").add(mapOf("name" to tag)).addOnSuccessListener {
            loadTags()
        }
    }

    fun deleteTag(tag: String) {
        db.collection("tags")
            .whereEqualTo("name", tag)
            .get()
            .addOnSuccessListener { result ->
                val batch = db.batch()
                result.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    loadTags()
                }
            }
    }
}