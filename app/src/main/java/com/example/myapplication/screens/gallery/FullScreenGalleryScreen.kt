package com.example.myapplication.screens.gallery

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenGalleryScreen(
    incidentId: String,
    startPosition: Int = 0,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var mediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    val pagerState = rememberPagerState(initialPage = startPosition, pageCount = { mediaUrls.size })

    // Load images from Firestore
    LaunchedEffect(incidentId) {
        FirebaseFirestore.getInstance()
            .collection("incidents")
            .document(incidentId)
            .get()
            .addOnSuccessListener { doc ->
                mediaUrls = doc.get("images") as? List<String> ?: emptyList()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load images", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (mediaUrls.isNotEmpty()) {
                HorizontalPager(state = pagerState) { page ->
                    Image(
                        painter = rememberAsyncImagePainter(model = mediaUrls[page]),
                        contentDescription = "Media Image $page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
