package com.gandhasiri.app.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PinLoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)

    val farmerName = prefs.getString("farmer_name", "Farmer") ?: "Farmer"
    val savedPin = prefs.getString("farmer_pin", "") ?: ""
    val savedPhone = prefs.getString("farmer_phone", "") ?: ""
    val photoPath = prefs.getString("farmer_photo_path", null)

    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    var showTooManyAttemptsDialog by remember { mutableStateOf(false) }
    
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    val titleText = com.gandhasiri.app.ui.theme.NearBlackBrown
    val bodyText = com.gandhasiri.app.ui.theme.DarkWood
    val background = com.gandhasiri.app.ui.theme.WarmCream
    val accent = com.gandhasiri.app.ui.theme.Sandalwood
    val errorColor = com.gandhasiri.app.ui.theme.PanicRed
    val surface = com.gandhasiri.app.ui.theme.PureWhite
    val outline = com.gandhasiri.app.ui.theme.PaleWood

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    // Shake Animation function
    suspend fun shake() {
        isError = true
        for (i in 0..5) {
            shakeOffset.animateTo(
                targetValue = if (i % 2 == 0) 15f else -15f,
                animationSpec = tween(durationMillis = 50)
            )
        }
        shakeOffset.animateTo(0f, animationSpec = tween(durationMillis = 50))
        pin = ""
    }

    Box(modifier = Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Title
            Text(
                text = "Welcome back,\n$farmerName",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleText,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Profile Photo
            if (!photoPath.isNullOrEmpty()) {
                AsyncImage(
                    model = Uri.parse(photoPath),
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, accent, CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(3.dp, accent)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Default Profile",
                        tint = accent,
                        modifier = Modifier.padding(24.dp).fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Enter your 4 digit PIN",
                fontSize = 16.sp,
                color = bodyText,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Input Boxes
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickable { focusRequester.requestFocus() }
            ) {
                BasicTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                            isError = false
                            if (pin.length == 4) {
                                if (pin == savedPin) {
                                    onLoginSuccess()
                                } else {
                                    attempts++
                                    if (attempts >= 5) {
                                        showTooManyAttemptsDialog = true
                                    } else {
                                        coroutineScope.launch { shake() }
                                    }
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .alpha(0f) // Hide actual input, overlay UI below
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        val isCurrent = i == pin.length
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isFilled) bodyText else surface, RoundedCornerShape(12.dp))
                                .border(
                                    width = 2.dp,
                                    color = when {
                                        isError -> errorColor
                                        isCurrent -> accent
                                        isFilled -> bodyText
                                        else -> outline
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFilled) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(surface))
                            }
                        }
                    }
                }
            }

            // Error Text
            Spacer(modifier = Modifier.height(16.dp))
            if (isError) {
                Text(
                    text = "Wrong PIN. Try again.",
                    color = errorColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(text = " ", fontSize = 14.sp) // Maintain space
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Forgot PIN Link
            Text(
                text = "Forgot PIN?",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showForgotPinDialog = true }
            )
        }
    }

    // Too Many Attempts Dialog
    if (showTooManyAttemptsDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Too many attempts", color = titleText, fontWeight = FontWeight.Bold) },
            text = { Text("Please reset the app or contact support.", color = bodyText) },
            confirmButton = {
                TextButton(onClick = { showTooManyAttemptsDialog = false }) {
                    Text("OK", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = surface
        )
    }

    // Forgot PIN Dialog
    if (showForgotPinDialog) {
        var phoneInput by remember { mutableStateOf("") }
        var phoneError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title = { Text("Verify Identity", color = titleText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your registered phone number to reset your PIN.", color = bodyText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it; phoneError = false },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = titleText,
                            unfocusedTextColor = titleText,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = outline,
                            focusedLabelColor = bodyText,
                            unfocusedLabelColor = bodyText.copy(alpha = 0.6f)
                        )
                    )
                    if (phoneError) {
                        Text("Phone number does not match.", color = errorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneInput == savedPhone) {
                            showForgotPinDialog = false
                            showResetPinDialog = true
                        } else {
                            phoneError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinDialog = false }) {
                    Text("Cancel", color = bodyText.copy(alpha = 0.6f))
                }
            },
            containerColor = surface
        )
    }

    // Reset PIN Dialog/Screen
    if (showResetPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Set New PIN", color = titleText, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Enter a new 4-digit PIN", color = bodyText)
                    Spacer(modifier = Modifier.height(24.dp))
                    BasicTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                newPin = it
                                if (newPin.length == 4) {
                                    // Save new PIN
                                    prefs.edit().putString("farmer_pin", newPin).apply()
                                    showResetPinDialog = false
                                    pin = ""
                                    isError = false
                                    attempts = 0
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = {
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                for (i in 0 until 4) {
                                    val isFilled = i < newPin.length
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isFilled) bodyText else surface, RoundedCornerShape(8.dp))
                                            .border(2.dp, if (i == newPin.length) accent else if (isFilled) bodyText else outline, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isFilled) Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(surface))
                                    }
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = { }, // Auto closes when 4 digits entered
            dismissButton = {
                TextButton(onClick = { showResetPinDialog = false }) {
                    Text("Cancel", color = bodyText.copy(alpha = 0.6f))
                }
            },
            containerColor = surface
        )
    }
}
