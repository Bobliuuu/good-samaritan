package com.example.myapplication.screens.incidentdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IncidentDetailScreen(
    viewModel: IncidentDetailViewModel,
    onBackPressed: () -> Unit,
    onImageClick: (Int) -> Unit
) {
    val meta by viewModel.incidentMeta.collectAsState()
    val mediaUrls by viewModel.mediaUrls.collectAsState()
    val voteCount by viewModel.voteCount.collectAsState()
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val userVoteValue by viewModel.userVoteValue.collectAsState()

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadVoteData(userId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Incident Details") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mediaUrls.isNotEmpty()) {
                Box(modifier = Modifier.testTag("MediaCarousel")) {
                    MediaCarousel(mediaUrls, onImageClick)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Box(modifier = Modifier.testTag("IncidentCard")) {
                IncidentCard(meta = meta)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Relevance: $voteCount", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Upvote
                Button(
                    onClick = {
                        val newVote = if (userVoteValue == 1) 0 else 1
                        viewModel.voteOnIncident(userId, newVote) {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userVoteValue == 1)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (userVoteValue == 1)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Yes")
                }

                // Downvote
                Button(
                    onClick = {
                        val newVote = if (userVoteValue == -1) 0 else -1
                        viewModel.voteOnIncident(userId, newVote) {}
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userVoteValue == -1)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (userVoteValue == -1)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("No")
                }
            }
        }
    }
}
