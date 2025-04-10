package com.example.myapplication.screens.addcontact

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.ViewModel

class AddContactViewModel(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    fun addContact(name: String, phoneNumber: String, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val numbersCollectionRef = db.collection("contacts").document(userId).collection("numbers")
        val newContact = mapOf("name" to name, "number" to phoneNumber)

        numbersCollectionRef.add(newContact)
            .addOnSuccessListener {
                Log.d("Firestore", "Contact added successfully")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding contact", e)
                onComplete()
            }
    }
}
