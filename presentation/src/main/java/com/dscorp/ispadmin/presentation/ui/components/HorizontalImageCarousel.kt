package com.dscorp.ispadmin.presentation.ui.components

import ZoomableImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun HorizontalImageCarousel(
    modifier: Modifier = Modifier,
    imageUrls: List<String>,
    onClose: () -> Unit,
    selectedImageIndex: Int
) {
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { imageUrls.size }, initialPage = selectedImageIndex)
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomableImage(
                modifier = Modifier.fillMaxSize(),
                imageUrl = imageUrls[page],
                onSwipeLeft = {
                    // Navigate to next image if not at the end
                    if (pagerState.currentPage < imageUrls.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                onSwipeRight = {
                    // Navigate to previous image if not at the beginning
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                }
            )
        }

        // Page indicator if there are multiple images
        if (imageUrls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(imageUrls.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        Color.White
                    else
                        Color.White.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(8.dp)
                            .background(color = color, shape = CircleShape)
                    )
                }
            }
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}