package com.gandhasiri.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandhasiri.app.viewmodel.TreesListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLegalClick: () -> Unit,
    onLogOut: () -> Unit,
    viewModel: TreesListViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)

    val background = com.gandhasiri.app.ui.theme.WarmCream
    val bodyText = com.gandhasiri.app.ui.theme.DarkWood
    val accent = com.gandhasiri.app.ui.theme.Sandalwood
    val panicRed = com.gandhasiri.app.ui.theme.PanicRed
    val surface = com.gandhasiri.app.ui.theme.PureWhite
    val topBarBg = com.gandhasiri.app.ui.theme.DarkWood

    var emergencyContact by remember { mutableStateOf(prefs.getString("emergency_contact", "Not set") ?: "Not set") }
    var securityAlertsEnabled by remember { mutableStateOf(prefs.getBoolean("security_alerts", true)) }

    var showClearDataWarning by remember { mutableStateOf(false) }
    var showPinSheet by remember { mutableStateOf(false) }
    var isEditingEmergencyContact by remember { mutableStateOf(false) }
    var newEmergencyContact by remember { mutableStateOf(emergencyContact) }

    val trees by viewModel.trees.collectAsState()
    val savedPin = prefs.getString("farmer_pin", "") ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
            )
        },
        containerColor = background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // --- SECURITY ---
            SettingsSectionHeader("Security", bodyText)
            
            // Emergency Contact
            SettingsItemRow("Emergency Contact", bodyText) {
                if (isEditingEmergencyContact) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = newEmergencyContact,
                            onValueChange = { newEmergencyContact = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = com.gandhasiri.app.ui.theme.NearBlackBrown,
                                unfocusedTextColor = com.gandhasiri.app.ui.theme.NearBlackBrown,
                                focusedBorderColor = accent,
                                unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isEditingEmergencyContact = false }) {
                                Text("Cancel", color = com.gandhasiri.app.ui.theme.MidBrown)
                            }
                            TextButton(onClick = {
                                emergencyContact = newEmergencyContact
                                prefs.edit().putString("emergency_contact", newEmergencyContact).apply()
                                isEditingEmergencyContact = false
                            }) {
                                Text("Save", color = accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text(text = emergencyContact, color = bodyText.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        newEmergencyContact = emergencyContact
                        isEditingEmergencyContact = true
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Contact", tint = accent)
                    }
                }
            }
            HorizontalDivider(color = com.gandhasiri.app.ui.theme.LightWood.copy(alpha = 0.5f))
            
            // App PIN
            SettingsItemRow("App PIN", bodyText) {
                TextButton(onClick = { showPinSheet = true }) {
                    Text("Change PIN", color = accent, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- NOTIFICATIONS ---
            SettingsSectionHeader("Notifications", bodyText)
            SettingsItemRow("Enable Security Alerts", bodyText) {
                Switch(
                    checked = securityAlertsEnabled,
                    onCheckedChange = {
                        securityAlertsEnabled = it
                        prefs.edit().putBoolean("security_alerts", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent,
                        uncheckedTrackColor = com.gandhasiri.app.ui.theme.PaleWood
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DATA ---
            SettingsSectionHeader("Data", bodyText)
            
            // Export Data
            SettingsItemRow("Export Tree Data", bodyText) {
                TextButton(onClick = {
                    val summary = buildString {
                        appendLine("GandhaSiri Tree Export")
                        appendLine("Total Trees: ${trees.size}")
                        appendLine("-----------------------")
                        trees.forEach {
                            appendLine("Tree ID: ${it.treeId}")
                            appendLine("Girth: ${it.girthCm}cm | Age: ${it.ageYears} yrs")
                            appendLine("-----------------------")
                        }
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, summary)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export Tree Data"))
                }) {
                    Text("Export", color = accent, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = com.gandhasiri.app.ui.theme.LightWood.copy(alpha = 0.5f))

            // Clear All Data
            SettingsItemRow("Clear All Data", bodyText) {
                TextButton(onClick = { showClearDataWarning = true }) {
                    Text("Clear", color = panicRed, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = com.gandhasiri.app.ui.theme.LightWood.copy(alpha = 0.5f))

            // App Version
            SettingsItemRow("App Version", bodyText) {
                Text("1.0.0 (Beta)", color = bodyText.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ABOUT ---
            SettingsSectionHeader("About", bodyText)
            SettingsItemRow("Project Name", bodyText) {
                Text(
                    text = "GandhaSiri —\nSandalwood Farmer's Guard",
                    color = bodyText.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp
                )
            }
            HorizontalDivider(color = com.gandhasiri.app.ui.theme.LightWood.copy(alpha = 0.5f))
            SettingsItemRow("Legal Guide", bodyText) {
                TextButton(onClick = onLegalClick) {
                    Text("Read", color = accent, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- LOG OUT ---
            Button(
                onClick = onLogOut,
                colors = ButtonDefaults.buttonColors(containerColor = bodyText),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // --- DIALOGS & BOTTOM SHEETS ---

    if (showClearDataWarning) {
        AlertDialog(
            onDismissRequest = { showClearDataWarning = false },
            title = { Text("Are you sure?", color = com.gandhasiri.app.ui.theme.NearBlackBrown, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all trees, measurements, and alerts and cannot be undone.", color = bodyText.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showClearDataWarning = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = panicRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataWarning = false }) {
                    Text("Cancel", color = com.gandhasiri.app.ui.theme.MidBrown)
                }
            },
            containerColor = surface
        )
    }

    if (showPinSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPinSheet = false },
            containerColor = surface
        ) {
            ChangePinContent(
                savedPin = savedPin,
                onSuccess = { newPin ->
                    prefs.edit().putString("farmer_pin", newPin).apply()
                    showPinSheet = false
                },
                onCancel = { showPinSheet = false }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = color.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsItemRow(title: String, textColor: Color, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(16.dp))
        content()
    }
}
