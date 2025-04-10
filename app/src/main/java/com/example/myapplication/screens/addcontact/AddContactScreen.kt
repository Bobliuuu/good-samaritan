package com.example.myapplication.ui.screens.contacts

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.screens.addcontact.AddContactViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddContactScreen(
    onContactAdded: () -> Unit,
    viewModel: AddContactViewModel = koinViewModel()
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add New Contact",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (XXX-XXX-XXXX)") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            "Format: XXX-XXX-XXXX",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isBlank() || phoneNumber.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else if (!phoneNumber.matches(Regex("\\d{3}-\\d{3}-\\d{4}"))) {
                    Toast.makeText(context, "Invalid phone number format! Use XXX-XXX-XXXX", Toast.LENGTH_SHORT).show()
                } else {
                    isLoading = true
                    viewModel.addContact(name, phoneNumber) {
                        isLoading = false
                        Toast.makeText(context, "Contact added successfully!", Toast.LENGTH_SHORT).show()
                        onContactAdded()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Save Contact", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
