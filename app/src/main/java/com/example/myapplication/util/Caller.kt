package com.example.myapplication.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.telecom.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

@RequiresApi(Build.VERSION_CODES.O)
class Caller : ConnectionService() {

    override fun onCreateIncomingConnection(
        phoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d("ConnectionService", "Incoming call received")
        return createVoipConnection(request)
    }

    override fun onCreateOutgoingConnection(
        phoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d("ConnectionService", "Outgoing call started")
        return createVoipConnection(request)
    }

    private fun createVoipConnection(request: ConnectionRequest?): Connection {
        val connection = object : Connection() {
            override fun onAnswer() {
                Log.d("ConnectionService", "Call answered")
                setActive()
            }

            override fun onDisconnect() {
                Log.d("ConnectionService", "Call disconnected")
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
            }
        }

        connection.setConnectionCapabilities(
            Connection.CAPABILITY_SUPPORT_HOLD.toInt()
        )

        connection.setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setInitialized()
        return connection
    }

    companion object {
        @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
        fun registerSelfManagedPhoneAccount(context: Context) {
            val telecomManager = context.getSystemService(TelecomManager::class.java)

            val componentName = ComponentName(context, Caller::class.java)
            val phoneAccountHandle = PhoneAccountHandle(componentName, context.packageName + ".SELF_MANAGED_PHONE_ACCOUNT")

            val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "VoIP Calls")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                .setHighlightColor(0xff4285F4.toInt())
                .setShortDescription("VoIP Service")
                .build()

            telecomManager?.registerPhoneAccount(phoneAccount)

            Log.d("Caller", "Self-managed phone account registered")
        }
    }

}
