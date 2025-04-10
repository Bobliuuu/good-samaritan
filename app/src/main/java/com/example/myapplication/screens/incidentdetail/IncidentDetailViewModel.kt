package com.example.myapplication.screens.incidentdetail

import androidx.lifecycle.ViewModel
import com.example.myapplication.model.IncidentMeta
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class IncidentDetailViewModel(
    private val db: FirebaseFirestore,
    private val incidentId: String
) : ViewModel() {

    private val _mediaUrls = MutableStateFlow<List<String>>(emptyList())
    open val mediaUrls: StateFlow<List<String>> = _mediaUrls

    private val _incidentMeta = MutableStateFlow(
        IncidentMeta(
            id = "", name = "", severity = "", description = "", tags = emptyList(), location = ""
        )
    )
    open val incidentMeta: StateFlow<IncidentMeta> = _incidentMeta

    private val _voteCount = MutableStateFlow(0)
    open val voteCount: StateFlow<Int> = _voteCount

    private val _hasUserVoted = MutableStateFlow(false)
    val hasUserVoted: StateFlow<Boolean> = _hasUserVoted

    open val userVoteValue = MutableStateFlow<Int?>(null) // -1, 1, or null

    init {
        loadIncident()
    }

    private fun loadIncident() {
        db.collection("incidents").document(incidentId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Unknown"
                val severity = doc.get("severity")?.toString() ?: "Unknown"
                val description = doc.getString("description") ?: "No description"
                val images = doc.get("images") as? List<String> ?: emptyList()
                val tags = doc.get("tags") as? List<String> ?: emptyList()
                val votes = doc.getLong("votes")?.toInt() ?: 0
                val location = doc.getString("location") ?: "Canada"

                _incidentMeta.value = IncidentMeta(
                    id = incidentId,
                    name = name,
                    severity = severity,
                    description = description,
                    tags = tags,
                    location = location
                )
                _mediaUrls.value = images
                _voteCount.value = votes
            }
    }

    open fun loadVoteData(userId: String) {
        val incidentRef = db.collection("incidents").document(incidentId)
        val voteRef = incidentRef.collection("votes").document(userId)
        voteRef.get().addOnSuccessListener {
            _hasUserVoted.value = it.exists()
            userVoteValue.value = it.getLong("value")?.toInt()
        }

        incidentRef.get().addOnSuccessListener {
            _voteCount.value = it.getLong("votes")?.toInt() ?: 0
        }

        voteRef.get().addOnSuccessListener {
            _hasUserVoted.value = it.exists()
        }
    }

    open fun voteOnIncident(userId: String, voteValue: Int, onComplete: (Boolean) -> Unit) {
        val incidentRef = db.collection("incidents").document(incidentId)
        val userVoteRef = incidentRef.collection("votes").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(incidentRef)
            val currentVotes = snapshot.getLong("votes") ?: 0

            val userVoteDoc = transaction.get(userVoteRef)
            val previousVote = userVoteDoc.getLong("value")?.toInt()

            when {
                previousVote == null -> {
                    // First time voting
                    transaction.update(incidentRef, "votes", currentVotes + voteValue)
                    transaction.set(userVoteRef, mapOf("value" to voteValue))
                }

                previousVote == voteValue -> {
                    // Same vote again → remove vote (optional)
                    transaction.update(incidentRef, "votes", currentVotes - voteValue)
                    transaction.delete(userVoteRef)
                }

                else -> {
                    // Different vote → switch
                    transaction.update(incidentRef, "votes", currentVotes - previousVote + voteValue)
                    transaction.set(userVoteRef, mapOf("value" to voteValue))
                }
            }
        }.addOnSuccessListener {
            loadVoteData(userId)
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

}
