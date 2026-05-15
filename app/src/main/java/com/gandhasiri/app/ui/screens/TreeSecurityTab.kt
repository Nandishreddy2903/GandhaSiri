package com.gandhasiri.app.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.gandhasiri.app.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.gandhasiri.app.viewmodel.TreeDetailViewModel
import kotlinx.coroutines.launch

private const val PREFS_SECURITY = "SecurityChecklist"
private const val KEY_EMERGENCY_CONTACT = "emergency_contact"
private const val CHANNEL_ID = "gandhasiri_security"

@Composable
fun TreeSecurityTab(
    treeId: String,
    viewModel: TreeDetailViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sharedPref = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)

    // --- Emergency Contact State ---
    var contactInput by remember { mutableStateOf(sharedPref.getString(KEY_EMERGENCY_CONTACT, "") ?: "") }
    var savedContact by remember { mutableStateOf(sharedPref.getString(KEY_EMERGENCY_CONTACT, "") ?: "") }

    // --- Checklist ---
    val checklistItems = listOf(
        R.string.checklist_item_1,
        R.string.checklist_item_2,
        R.string.checklist_item_3,
        R.string.checklist_item_4,
        R.string.checklist_item_5
    )
    val checkboxStates = remember {
        mutableStateListOf<Boolean>().apply {
            checklistItems.indices.forEach { index ->
                add(sharedPref.getBoolean("${treeId}_security_$index", false))
            }
        }
    }
    val completedCount = checkboxStates.count { it }
    val progress = if (checklistItems.isNotEmpty()) completedCount.toFloat() / checklistItems.size else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // --- Panic Button Animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var isShaking by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (isShaking) 10f else 0f,
        animationSpec = repeatable(
            iterations = 5,
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake",
        finishedListener = { isShaking = false }
    )

    // --- Observe ViewModel location (set via prefetchPanicLocation) ---
    val panicLocation by viewModel.panicLocation.collectAsState()
    val cachedLat = panicLocation?.first
    val cachedLng = panicLocation?.second

    // --- Permission Launchers ---
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        // After user grants permission, kick off the ViewModel location fetch
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.prefetchPanicLocation(context)
        }
    }

    // Trigger location pre-fetch as soon as the Security tab is opened
    LaunchedEffect(Unit) {
        val fineGranted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permission already granted — fetch via ViewModel immediately
            viewModel.prefetchPanicLocation(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.gandhasiri.app.ui.theme.WarmCream)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // =====================================================
        // SECTION 0: Emergency Contact
        // =====================================================
        Text(
            text = "Emergency Contact",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = com.gandhasiri.app.ui.theme.NearBlackBrown
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter one phone number to receive WhatsApp alerts when the panic button is pressed.",
            style = MaterialTheme.typography.bodySmall,
            color = com.gandhasiri.app.ui.theme.DarkWood
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = contactInput,
                        onValueChange = { contactInput = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91XXXXXXXXXX") },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = com.gandhasiri.app.ui.theme.Sandalwood)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.gandhasiri.app.ui.theme.Sandalwood,
                            unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood,
                            focusedLabelColor = com.gandhasiri.app.ui.theme.Sandalwood,
                            unfocusedLabelColor = com.gandhasiri.app.ui.theme.NearBlackBrown,
                            focusedTextColor = com.gandhasiri.app.ui.theme.NearBlackBrown,
                            unfocusedTextColor = com.gandhasiri.app.ui.theme.NearBlackBrown
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val trimmed = contactInput.trim()
                            sharedPref.edit().putString(KEY_EMERGENCY_CONTACT, trimmed).apply()
                            savedContact = trimmed
                            Toast.makeText(context, "Contact saved!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.gandhasiri.app.ui.theme.Sandalwood,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                if (savedContact.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Currently alerting: $savedContact",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = com.gandhasiri.app.ui.theme.MidBrown
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // =====================================================
        // SECTION 1: Safety Checklist
        // =====================================================
        Text(
            text = "Security Checklist",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = com.gandhasiri.app.ui.theme.NearBlackBrown
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Farm Security Score", fontWeight = FontWeight.Bold, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
                    Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Black, color = com.gandhasiri.app.ui.theme.Sandalwood)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = com.gandhasiri.app.ui.theme.Sandalwood,
                    trackColor = com.gandhasiri.app.ui.theme.WarmCream
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                checklistItems.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (checkboxStates[index]) com.gandhasiri.app.ui.theme.WarmCream.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val isChecked = !checkboxStates[index]
                                checkboxStates[index] = isChecked
                                sharedPref.edit().putBoolean("${treeId}_security_$index", isChecked).apply()
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkboxStates[index],
                            onCheckedChange = { isChecked ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                checkboxStates[index] = isChecked
                                sharedPref.edit().putBoolean("${treeId}_security_$index", isChecked).apply()
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = com.gandhasiri.app.ui.theme.Sandalwood,
                                uncheckedColor = com.gandhasiri.app.ui.theme.MidBrown
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(text),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (checkboxStates[index]) FontWeight.Bold else FontWeight.Normal,
                            color = if (checkboxStates[index]) com.gandhasiri.app.ui.theme.NearBlackBrown else com.gandhasiri.app.ui.theme.DarkWood
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // =====================================================
        // SECTION 2: Panic Button
        // =====================================================
        Text(
            text = stringResource(R.string.emergency_alert_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = com.gandhasiri.app.ui.theme.PanicRed
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.emergency_alert_desc),
            style = MaterialTheme.typography.bodySmall,
            color = com.gandhasiri.app.ui.theme.DarkWood
        )
        
        // GPS Status Indicator + Retry
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (cachedLat != null) Color(0xFF4CAF50) else Color(0xFFFF9800))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (cachedLat != null)
                    stringResource(R.string.gps_ready) + " — %.5f, %.5f".format(cachedLat, cachedLng)
                else
                    stringResource(R.string.acquiring_gps),
                fontSize = 11.sp,
                color = if (cachedLat != null) Color(0xFF4CAF50) else Color(0xFFFF9800),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (cachedLat == null) {
                TextButton(
                    onClick = { viewModel.prefetchPanicLocation(context) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.retry), fontSize = 11.sp, color = com.gandhasiri.app.ui.theme.Sandalwood, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (cachedLat == null) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.gps_not_available_warning),
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
                            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.open), fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isShaking = true
                showEmergencyDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .graphicsLayer {
                    translationX = shakeOffset
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.PanicRed),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp)
            )
            Text(
                stringResource(R.string.panic_button_label),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }

        // =====================================================
        // Confirmation Dialog
        // =====================================================
        if (showEmergencyDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                title = {
                    Text(
                        "Confirm Emergency Alert",
                        color = com.gandhasiri.app.ui.theme.PanicRed,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "This will notify your emergency contact on WhatsApp and send a local notification. Are you sure?",
                        color = com.gandhasiri.app.ui.theme.NearBlackBrown
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showEmergencyDialog = false

                            // 1. Fire local notification immediately
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel = NotificationChannel(
                                    CHANNEL_ID,
                                    "Security Alerts — GandhaSiri",
                                    NotificationManager.IMPORTANCE_HIGH
                                ).apply { description = "Emergency security notifications" }
                                notificationManager.createNotificationChannel(channel)
                            }
                            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setContentTitle("Security Alert — GandhaSiri")
                                .setContentText("Panic button pressed. Your neighbor has been notified via WhatsApp.")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true)
                                .build()
                            
                            val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            } else true

                            if (hasNotifPermission) {
                                notificationManager.notify(202, notification)
                            }

                            // 2. Send WhatsApp alert instantly with cached location
                            sendWhatsAppAlert(
                                context = context,
                                sharedPref = sharedPref,
                                viewModel = viewModel,
                                lat = cachedLat,
                                lng = cachedLng
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.PanicRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Yes, Alert", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyDialog = false }) {
                        Text("Cancel", color = com.gandhasiri.app.ui.theme.MidBrown)
                    }
                },
                containerColor = com.gandhasiri.app.ui.theme.PureWhite
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

private fun sendWhatsAppAlert(
    context: Context,
    sharedPref: android.content.SharedPreferences,
    viewModel: TreeDetailViewModel,
    lat: Double?,
    lng: Double?
) {
    val phoneNumber = sharedPref.getString(KEY_EMERGENCY_CONTACT, "") ?: ""

    val locationText = if (lat != null && lng != null && (lat != 0.0 || lng != 0.0))
        "My location: https://maps.google.com/?q=$lat,$lng"
    else
        "Location could not be determined at this time."

    val message = "URGENT - GandhaSiri Security Alert.\n" +
        "Suspicious activity detected near my sandalwood farm. " +
        "Please check immediately.\n$locationText"

    // 3. Open WhatsApp, fall back to SMS
    val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").removePrefix("+")
    try {
        // Using direct whatsapp scheme for faster redirect
        val url = "whatsapp://send?phone=$cleanNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            // Fallback to wa.me if whatsapp:// fails
            val webUrl = "https://wa.me/$cleanNumber?text=${Uri.encode(message)}"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open WhatsApp or SMS app.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 4. Log to AlertLog Room DB
    val note = "PANIC alert. Location: lat=${lat ?: "N/A"}, lng=${lng ?: "N/A"}. Contacted: $phoneNumber"
    viewModel.insertAlert(note = note, type = "PANIC")

    Toast.makeText(context, "Neighbor alerted successfully.", Toast.LENGTH_SHORT).show()
}
