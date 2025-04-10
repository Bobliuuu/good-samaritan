package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.GoodSamaritanTheme
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import androidx.activity.compose.rememberLauncherForActivityResult

class SignInActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google Sign-In config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            GoodSamaritanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignInScreen(
                        googleSignInClient = googleSignInClient,
                        onSignInSuccess = { navigateToMainActivity() }
                    )
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}

private fun navigateAfterLogin(
    doc: DocumentSnapshot?,
    context: Context,
    activity: Activity?
) {
    val isAdmin = doc?.getBoolean("admin") == true
    val intent = Intent(context, MainActivity::class.java)
    intent.putExtra("isAdmin", isAdmin)
    context.startActivity(intent)
    activity?.finish()
}

@Composable
fun SignInScreen(
    googleSignInClient: GoogleSignInClient,
    onSignInSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val activity = context as? Activity

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val userDoc = db.collection("users").document(user.uid)
                            userDoc.get().addOnSuccessListener { doc ->
                                if (!doc.exists()) {
                                    userDoc.set(mapOf("admin" to false)).addOnSuccessListener {
                                        navigateAfterLogin(doc = null, context, activity)
                                    }
                                } else {
                                    navigateAfterLogin(doc, context, activity)
                                }
                            }
                        }
                    } else {
                        errorMessage = "Google Sign-In failed: ${task.exception?.message}"
                    }
                }
        } catch (e: ApiException) {
            errorMessage = "Google Sign-In error: ${e.message}"
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign In", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
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

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }

                    val isAdminLogin = email == "admin" && password == "admin"
                    if (!isAdminLogin && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Invalid email format"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val (finalEmail, finalPassword) = if (isAdminLogin) {
                        context.getString(R.string.admin_email) to context.getString(R.string.admin_password)
                    } else {
                        email to password
                    }

                    auth.signInWithEmailAndPassword(finalEmail, finalPassword)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    val userDoc = db.collection("users").document(user.uid)
                                    userDoc.get().addOnSuccessListener { snapshot ->
                                        if (!snapshot.exists()) {
                                            userDoc.set(mapOf("admin" to false))
                                        }
                                    }

                                    userDoc.get().addOnSuccessListener { doc ->
                                        val isAdmin = doc.getBoolean("admin") == true
                                        val intent = Intent(context, MainActivity::class.java)
                                        intent.putExtra("isAdmin", isAdmin)
                                        context.startActivity(intent)
                                        activity?.finish()
                                    }
                                } else {
                                    errorMessage = "User not found"
                                }
                            } else {
                                errorMessage = "Login failed: ${task.exception?.message}"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().testTag("signInButton"),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Sign In")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Google")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val intent = Intent(context, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    activity?.finish()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
