package com.example.myapplication.screens.contact

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.contactsapp.model.Contact
import com.example.myapplication.navigation.ContactsViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material.icons.filled.Send

@Composable
fun ContactsScreen(
    navController: NavHostController,
    viewModel: ContactsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchContacts()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(contacts) { contact ->
            ContactItem(contact, context) {
                viewModel.deleteContact(contact) {
                    Toast.makeText(context, "Deleted ${contact.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate("add_contact") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Add Contact",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ContactItem(contact: Contact, context: Context, onDelete: () -> Unit) {
    val viewModel: ContactsViewModel = koinViewModel()
    val callLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCall(context, contact.phoneNumber)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    callLauncher.launch(Manifest.permission.CALL_PHONE)
                }) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = {
                    if (latitude != null && longitude != null) {
                        val message = "Hey! Come pick me up: https://maps.google.com/?q=$latitude,$longitude"
                        viewModel.sendSms(context, contact.phoneNumber, message)
                    } else {
                        viewModel.getCurrentLocation(context)
                        Toast.makeText(context, "Fetching location… try again", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Text", tint = MaterialTheme.colorScheme.secondary)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun startCall(context: Context, phoneNumber: String) {
    val telecomManager = context.getSystemService(TelecomManager::class.java)
    val phoneAccountHandle = getDefaultPhoneAccount(context)

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "Call permission not granted", Toast.LENGTH_SHORT).show()
        return
    }

    if (telecomManager != null && phoneAccountHandle != null) {
        val uri = Uri.fromParts("tel", phoneNumber, null)
        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
        }
        try {
            telecomManager.placeCall(uri, extras)
        } catch (e: SecurityException) {
            Log.e("Telecom", "Call failed due to missing permissions", e)
            Toast.makeText(context, "Call failed: Permission denied", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Self-Managed Calling Not Available", Toast.LENGTH_SHORT).show()
    }
}

fun getDefaultPhoneAccount(context: Context): PhoneAccountHandle? {
    val telecomManager = context.getSystemService(TelecomManager::class.java) ?: return null

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        Log.e("TAG", "READ_PHONE_STATE permission denied")
        return null
    }

    val phoneAccounts = telecomManager.callCapablePhoneAccounts
    Log.d("TAG", "Available Phone Accounts: $phoneAccounts")

    return phoneAccounts.firstOrNull()
}
