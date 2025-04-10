package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.ui.theme.GoodSamaritanTheme

class SignUpActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GoodSamaritanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignUpScreen(onSignUpSuccess = {
                        navigateToHomeActivity()
                    })
                }
            }
        }
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Close SignInActivity forever once sign in is done
    }
}

@Composable
fun SignUpScreen(onSignUpSuccess: () -> Unit) {
    // Logcat only
    val TAG = "SIGNUP"

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // var failed = false // DEBUG ONLY

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign Up", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!errorMessage.isNullOrEmpty()) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Invalid email format"
                        return@Button
                    }

                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_LONG).show()
                                onSignUpSuccess()
                            } else {
                                errorMessage = "Registration failed: ${task.exception?.message}"
                                Log.e(TAG, "SIGNUP FAILED: ${task.exception?.message}")
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    onSignUpSuccess()
                },
                modifier = Modifier.fillMaxWidth()
            )
            {
                Text("Back")
            }
        }
    }
}
