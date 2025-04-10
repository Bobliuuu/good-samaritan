package com.example.myapplication.admin.adminincident

import androidx.lifecycle.ViewModel
import com.example.myapplication.model.AdminIncident
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class AdminIncidentViewModel(private val db: FirebaseFirestore) : ViewModel() {
    private val _incidents = MutableStateFlow<List<AdminIncident>>(emptyList())
    open val incidents: StateFlow<List<AdminIncident>> = _incidents

    init {
        loadIncidents()
    }

    private fun loadIncidents() {
        db.collection("incidents")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val incidentList = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val votes = doc.getLong("votes")?.toInt() ?: 0
                        AdminIncident(doc.id, name, votes)
                    }
                    _incidents.value = incidentList
                }
            }
    }

    open fun deleteIncident(id: String) {
        db.collection("incidents").document(id).delete()
    }
}
