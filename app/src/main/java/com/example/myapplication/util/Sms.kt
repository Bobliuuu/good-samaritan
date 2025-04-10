package com.example.myapplication.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

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
