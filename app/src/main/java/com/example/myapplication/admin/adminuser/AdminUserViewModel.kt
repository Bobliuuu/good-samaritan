package com.example.myapplication.admin.adminuser

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray

data class AdminUser(
    val uid: String,
    val email: String,
    val createdAt: String,
    val lastSignIn: String,
    val provider: List<String>
)

class AdminUserViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<AdminUser>>(emptyList())
    val users: StateFlow<List<AdminUser>> = _users

    private val client = OkHttpClient()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            val response = fetchUsersFromApi()
            if (response != null) {
                _users.value = response
            } else {
                Log.e("AdminUser", "Failed to load users")
            }
        }
    }

    private suspend fun fetchUsersFromApi(): List<AdminUser>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://firebase-auth-api-690560323403.us-central1.run.app/list_users")
                .get()
                .build()

            val response = client.newCall(request).execute()

            val body = response.body?.string() ?: return@withContext null
            val jsonArray = JSONArray(body)

            val users = mutableListOf<AdminUser>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                users.add(
                    AdminUser(
                        uid = obj.getString("uid"),
                        email = obj.optString("email", "(anonymous)"),
                        createdAt = obj.optString("createdAt", "-"),
                        lastSignIn = obj.optString("lastSignIn", "-"),
                        provider = obj.optJSONArray("provider")?.let { arr ->
                            List(arr.length()) { arr.getString(it) }
                        } ?: emptyList()
                    )
                )
            }

            return@withContext users
        } catch (e: Exception) {
            Log.e("AdminUser", "Error fetching users", e)
            return@withContext null
        }
    }

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            val success = deleteUserFromApi(uid)
            if (success) {
                _users.value = _users.value.filterNot { it.uid == uid }
            } else {
                Log.e("AdminUser", "Failed to delete user $uid")
            }
        }
    }

    private suspend fun deleteUserFromApi(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://firebase-auth-api-690560323403.us-central1.run.app/delete_user?uid=$uid")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e("AdminUser", "Error deleting user $uid", e)
            return@withContext false
        }
    }
}
