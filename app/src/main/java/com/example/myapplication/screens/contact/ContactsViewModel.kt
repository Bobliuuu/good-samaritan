package com.example.myapplication.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.contactsapp.model.Contact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.StateFlow
import com.google.android.gms.location.FusedLocationProviderClient

class ContactsViewModel(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()
    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private fun initLocation(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }


    fun getCurrentLocation(context: Context) {
        initLocation(context)
        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
            if (location != null) {
                _latitude.value = location.latitude
                _longitude.value = location.longitude
            }
        }
    }

    fun fetchContacts() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("contacts").document(userId).collection("numbers")
            .get()
            .addOnSuccessListener { result ->
                val contactsList = result.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    val phoneNumber = doc.getString("number")
                    if (name != null && phoneNumber != null) Contact(name, phoneNumber) else null
                }
                _contacts.value = contactsList
            }
    }

    fun deleteContact(contact: Contact, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("contacts").document(userId).collection("numbers")
            .whereEqualTo("name", contact.name)
            .whereEqualTo("number", contact.phoneNumber)
            .get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    document.reference.delete().addOnSuccessListener {
                        fetchContacts()
                        onComplete()
                    }
                }
            }
    }

    fun sendSms(context: Context, phoneNumber: String, message: String) {
        val smsUri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No SMS app found.", Toast.LENGTH_SHORT).show()
            Log.e("SMS", "Failed to send SMS", e)
        }
    }

}
