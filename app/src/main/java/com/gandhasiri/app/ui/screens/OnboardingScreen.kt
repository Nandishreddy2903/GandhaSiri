package com.gandhasiri.app.ui.screens
import com.gandhasiri.app.R
import androidx.compose.ui.res.stringResource

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun onboardingSlides() = listOf(
    OnboardingSlide(
        title = stringResource(R.string.onboarding_slide1_title),
        description = stringResource(R.string.onboarding_slide1_desc),
        icon = Icons.Filled.Forest
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_slide2_title),
        description = stringResource(R.string.onboarding_slide2_desc),
        icon = Icons.Filled.TrendingUp
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_slide3_title),
        description = stringResource(R.string.onboarding_slide3_desc),
        icon = Icons.Filled.AutoAwesome
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_slide4_title),
        description = stringResource(R.string.onboarding_slide4_desc),
        icon = Icons.Filled.Security
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val slides = onboardingSlides()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    val background = com.gandhasiri.app.ui.theme.WarmCream
    val titleColor = com.gandhasiri.app.ui.theme.NearBlackBrown
    val descColor = com.gandhasiri.app.ui.theme.DarkWood
    val primaryAccent = com.gandhasiri.app.ui.theme.Sandalwood
    val secondaryAccent = com.gandhasiri.app.ui.theme.PaleWood

    Scaffold(
        containerColor = background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(
                    visible = pagerState.currentPage < slides.size - 1,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(slides.size - 1)
                        }
                    }) {
                        Text(stringResource(R.string.onboarding_skip), color = descColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Illustration Area (60%)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(200.dp),
                            shape = CircleShape,
                            color = primaryAccent.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier
                                    .padding(48.dp)
                                    .fillMaxSize()
                            )
                        }
                    }

                    // Text Area (40%)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = slide.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = slide.description,
                            fontSize = 16.sp,
                            color = descColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // Bottom Navigation Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(slides.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) primaryAccent else secondaryAccent)
                        )
                    }
                }

                // Next / Get Started Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < slides.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            // Finish Onboarding
                            val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("onboarding_complete", true).apply()
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == slides.size - 1) stringResource(R.string.onboarding_get_started) else stringResource(R.string.onboarding_next),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
