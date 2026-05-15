package com.gandhasiri.app.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

@Composable
fun SplashScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)

    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }

    val background = com.gandhasiri.app.ui.theme.DarkWood
    val appNameColor = com.gandhasiri.app.ui.theme.PureWhite
    val taglineColor = com.gandhasiri.app.ui.theme.LightGold
    val leafIconColor = com.gandhasiri.app.ui.theme.Sandalwood

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(800, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(800, easing = LinearOutSlowInEasing)
                )
            }
        }
        delay(800)
        
        val isRegistered = prefs.contains("farmer_name")
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)
        val languageSet = prefs.contains("app_language")

        if (!languageSet) {
            onNavigate("language_selection")
        } else if (!onboardingComplete) {
            onNavigate("onboarding")
        } else if (isRegistered) {
            onNavigate("pin_login")
        } else {
            onNavigate("register")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
        ) {
            Icon(
                imageVector = Icons.Filled.Eco,
                contentDescription = "Sandalwood Leaf",
                modifier = Modifier.size(80.dp),
                tint = leafIconColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "GandhaSiri",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = appNameColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Farmer's Digital Guardian",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = taglineColor
            )
        }
    }
}
