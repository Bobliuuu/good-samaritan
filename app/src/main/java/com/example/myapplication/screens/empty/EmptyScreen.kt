package com.example.myapplication.screens.empty

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavHostController
import com.example.myapplication.screens.empty.EmptyViewModel

@Composable
fun EmptyScreen(
    navController: NavHostController,
    viewModel: EmptyViewModel = koinViewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {

    }
}

