package com.gandhasiri.app.ui.screens

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat

data class LanguageOption(
    val code: String,
    val nameInLang: String,
    val nameInEnglish: String
)

@Composable
fun LanguageSelectionScreen(onNavigateNext: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)
    
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    
    val languages = listOf(
        LanguageOption("kn", "ಕನ್ನಡ", "Kannada"),
        LanguageOption("hi", "हिंदी", "Hindi"),
        LanguageOption("ta", "தமிழ்", "Tamil"),
        LanguageOption("te", "తెలుగు", "Telugu"),
        LanguageOption("ml", "മലയാളം", "Malayalam"),
        LanguageOption("en", "English", "English")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.gandhasiri.app.ui.theme.WarmCream)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Icon(
            imageVector = Icons.Filled.Eco,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = com.gandhasiri.app.ui.theme.Sandalwood
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Choose Your Language",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = com.gandhasiri.app.ui.theme.NearBlackBrown
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val localHeadings = listOf(
            "ನಿಮ್ಮ ಭಾಷೆ ಆಯ್ಕೆ ಮಾಡಿ",
            "अपनी भाषा चुनें",
            "உங்கள் மொழியை தேர்ந்தெடுக்கவும்",
            "మీ భాషను ఎంచుకోండి",
            "നിങ്ങളുടെ ഭാഷ തിരഞ്ഞെടുക്കുക"
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            localHeadings.forEach { heading ->
                Text(
                    text = heading,
                    fontSize = 16.sp,
                    color = com.gandhasiri.app.ui.theme.MidBrown,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(languages) { lang ->
                LanguageCard(
                    language = lang,
                    isSelected = selectedLanguage == lang.code,
                    onClick = { selectedLanguage = lang.code }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                selectedLanguage?.let { code ->
                    prefs.edit().putString("app_language", code).apply()
                    
                    // Set app locale
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(code)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                    
                    onNavigateNext()
                }
            },
            enabled = selectedLanguage != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = com.gandhasiri.app.ui.theme.Sandalwood,
                disabledContainerColor = com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "CONTINUE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun LanguageCard(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, com.gandhasiri.app.ui.theme.Sandalwood) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nameInLang,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = com.gandhasiri.app.ui.theme.NearBlackBrown
                )
                Text(
                    text = language.nameInEnglish,
                    fontSize = 14.sp,
                    color = com.gandhasiri.app.ui.theme.MidBrown
                )
            }
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) com.gandhasiri.app.ui.theme.Sandalwood else com.gandhasiri.app.ui.theme.WarmCream)
                    .border(1.dp, com.gandhasiri.app.ui.theme.PaleWood, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
