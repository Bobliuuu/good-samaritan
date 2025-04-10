package com.example.myapplication.screens.incidentdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale

@Composable
fun MediaCarousel(mediaUrls: List<String>, onImageClick: (Int) -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { mediaUrls.size })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(state = pagerState) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clickable { onImageClick(page) }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(mediaUrls[page]),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            repeat(mediaUrls.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}
