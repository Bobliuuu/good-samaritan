package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.material3.*
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.GoodSamaritanTheme
import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.util.Caller
import com.example.myapplication.ui.components.MyApp

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            registerPhoneAccount()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isAdmin = intent.getBooleanExtra("isAdmin", false)

        checkPermissions()

        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            Toast.makeText(this, "Welcome ${it.email}", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show()
        }

        setContent {
            GoodSamaritanTheme {
                MyApp(isAdmin = isAdmin)
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        } else {
            registerPhoneAccount()
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun registerPhoneAccount() {
        Caller.registerSelfManagedPhoneAccount(this)
    }
}
