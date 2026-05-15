package com.gandhasiri.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.net.Uri
import coil.compose.AsyncImage
import com.gandhasiri.app.viewmodel.TreesListViewModel
import java.util.concurrent.TimeUnit
import androidx.compose.ui.res.stringResource
import com.gandhasiri.app.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: TreesListViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)

    // State for user details
    var name by remember { mutableStateOf(prefs.getString("farmer_name", "") ?: "") }
    var village by remember { mutableStateOf(prefs.getString("farmer_village", "") ?: "") }
    var district by remember { mutableStateOf(prefs.getString("farmer_district", "") ?: "") }
    var state by remember { mutableStateOf(prefs.getString("farmer_state", "") ?: "") }
    var phone by remember { mutableStateOf(prefs.getString("farmer_phone", "") ?: "") }
    var farmSize by remember { mutableStateOf(prefs.getString("farmer_farm_size", "") ?: "") }
    
    val photoPath = prefs.getString("farmer_photo_path", null)
    val savedPin = prefs.getString("farmer_pin", "") ?: ""

    // DB Stats
    val trees by viewModel.trees.collectAsState()
    val totalTrees = trees.size
    
    val daysSinceRegistration = if (trees.isNotEmpty()) {
        val firstTreeDate = trees.minByOrNull { it.createdAt }?.createdAt ?: System.currentTimeMillis()
        val diff = System.currentTimeMillis() - firstTreeDate
        TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
    } else 0

    // PIN Change State
    var showPinSheet by remember { mutableStateOf(false) }

    // Language Selection State
    var showLanguageSheet by remember { mutableStateOf(false) }

    val titleText = com.gandhasiri.app.ui.theme.NearBlackBrown
    val bodyText = com.gandhasiri.app.ui.theme.DarkWood
    val background = com.gandhasiri.app.ui.theme.WarmCream
    val accent = com.gandhasiri.app.ui.theme.Sandalwood
    val surface = com.gandhasiri.app.ui.theme.PureWhite
    val outline = com.gandhasiri.app.ui.theme.LightWood
    val topBarBg = com.gandhasiri.app.ui.theme.DarkWood

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = Color.White)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            if (!photoPath.isNullOrEmpty()) {
                AsyncImage(
                    model = Uri.parse(photoPath),
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, accent, CircleShape)
                )
            } else {
                Surface(
                    modifier = Modifier.size(120.dp),
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

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = titleText)
            Text(
                text = listOf(village, district).filter { it.isNotBlank() }.joinToString(", "),
                fontSize = 16.sp,
                color = bodyText.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- STATS ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(title = stringResource(R.string.total_trees_label), value = totalTrees.toString(), modifier = Modifier.weight(1f))
                StatCard(title = stringResource(R.string.farm_size_label), value = stringResource(R.string.acres_format, farmSize), modifier = Modifier.weight(1f))
                StatCard(title = stringResource(R.string.days_active_label), value = daysSinceRegistration.toString(), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- EDITABLE FIELDS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.personal_details), fontWeight = FontWeight.Bold, color = bodyText, modifier = Modifier.padding(bottom = 16.dp))

                    EditableProfileRow(stringResource(R.string.name_label), name) { newName ->
                        name = newName
                        prefs.edit().putString("farmer_name", newName).apply()
                    }
                    HorizontalDivider(color = outline.copy(alpha = 0.5f))
                    EditableProfileRow(stringResource(R.string.village_label), village) { newVillage ->
                        village = newVillage
                        prefs.edit().putString("farmer_village", newVillage).apply()
                    }
                    HorizontalDivider(color = outline.copy(alpha = 0.5f))
                    EditableProfileRow(stringResource(R.string.district_label), district) { newDist ->
                        district = newDist
                        prefs.edit().putString("farmer_district", newDist).apply()
                    }
                    HorizontalDivider(color = outline.copy(alpha = 0.5f))
                    EditableProfileRow(stringResource(R.string.state_label), state) { newState ->
                        state = newState
                        prefs.edit().putString("farmer_state", newState).apply()
                    }
                    HorizontalDivider(color = outline.copy(alpha = 0.5f))
                    EditableProfileRow(stringResource(R.string.phone_label), phone, isNumber = true) { newPhone ->
                        phone = newPhone
                        prefs.edit().putString("farmer_phone", newPhone).apply()
                    }
                    HorizontalDivider(color = outline.copy(alpha = 0.5f))
                    EditableProfileRow(stringResource(R.string.farm_size_acres), farmSize, isNumber = true) { newSize ->
                        farmSize = newSize
                        prefs.edit().putString("farmer_farm_size", newSize).apply()
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- CHANGE PIN BUTTON ---
            Button(
                onClick = { showPinSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = bodyText),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.change_pin), fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CHANGE LANGUAGE BUTTON ---
            Button(
                onClick = { showLanguageSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.change_language), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // --- PIN CHANGE BOTTOM SHEET ---
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

    // --- LANGUAGE CHANGE BOTTOM SHEET ---
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            containerColor = surface
        ) {
            LanguageSelectionContent(
                onLanguageSelected = { langCode ->
                    val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("app_language", langCode).apply()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
                    showLanguageSheet = false
                },
                onCancel = { showLanguageSheet = false }
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = com.gandhasiri.app.ui.theme.DarkWood)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, color = com.gandhasiri.app.ui.theme.MidBrown, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun EditableProfileRow(
    label: String,
    value: String,
    isNumber: Boolean = false,
    onSave: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf(value) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isEditing) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, color = com.gandhasiri.app.ui.theme.MidBrown)
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = com.gandhasiri.app.ui.theme.NearBlackBrown,
                        unfocusedTextColor = com.gandhasiri.app.ui.theme.NearBlackBrown,
                        focusedBorderColor = com.gandhasiri.app.ui.theme.Sandalwood,
                        unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false; editValue = value }) {
                        Text(stringResource(R.string.cancel), color = com.gandhasiri.app.ui.theme.MidBrown)
                    }
                    TextButton(onClick = { isEditing = false; onSave(editValue) }) {
                        Text(stringResource(R.string.save), color = com.gandhasiri.app.ui.theme.Sandalwood, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, color = com.gandhasiri.app.ui.theme.MidBrown)
                Text(value.ifEmpty { stringResource(R.string.not_set) }, fontSize = 16.sp, color = com.gandhasiri.app.ui.theme.DarkWood, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.options), tint = com.gandhasiri.app.ui.theme.Sandalwood)
            }
        }
    }
}

@Composable
fun ChangePinContent(savedPin: String, onSuccess: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Current, 2: New, 3: Confirm
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val bodyText = com.gandhasiri.app.ui.theme.DarkWood
    val accent = com.gandhasiri.app.ui.theme.Sandalwood
    val surface = com.gandhasiri.app.ui.theme.PureWhite
    val outline = com.gandhasiri.app.ui.theme.PaleWood

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (step) {
                1 -> stringResource(R.string.enter_current_pin)
                2 -> stringResource(R.string.enter_new_pin)
                else -> stringResource(R.string.confirm_new_pin)
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = bodyText
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Reusing the BasicTextField trick for visual boxes
        val pinValue = when (step) { 1 -> currentPin; 2 -> newPin; else -> confirmPin }
        val onPinChange: (String) -> Unit = { 
            errorMsg = ""
            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                when (step) {
                    1 -> {
                        currentPin = it
                        if (it.length == 4) {
                            if (it == savedPin) step = 2 else errorMsg = context.getString(R.string.incorrect_pin)
                        }
                    }
                    2 -> {
                        newPin = it
                        if (it.length == 4) step = 3
                    }
                    3 -> {
                        confirmPin = it
                        if (it.length == 4) {
                            if (it == newPin) onSuccess(it) else errorMsg = context.getString(R.string.pins_do_not_match)
                        }
                    }
                }
            }
        }

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        androidx.compose.foundation.text.BasicTextField(
            value = pinValue,
            onValueChange = onPinChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.focusRequester(focusRequester).size(1.dp).alpha(0f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.clickable { focusRequester.requestFocus() }
        ) {
            for (i in 0 until 4) {
                val isFilled = i < pinValue.length
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isFilled) bodyText else surface, RoundedCornerShape(12.dp))
                        .border(2.dp, if (i == pinValue.length) accent else if (isFilled) bodyText else outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFilled) Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(surface))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (errorMsg.isNotEmpty()) {
            Text(text = errorMsg, color = com.gandhasiri.app.ui.theme.PanicRed, fontSize = 14.sp)
        } else {
            Text(" ", fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.cancel), color = com.gandhasiri.app.ui.theme.MidBrown)
        }
    }
}

@Composable
fun LanguageSelectionContent(onLanguageSelected: (String) -> Unit, onCancel: () -> Unit) {
    val bodyText = com.gandhasiri.app.ui.theme.DarkWood
    val accent = com.gandhasiri.app.ui.theme.Sandalwood

    val languages = listOf(
        "kn" to "ಕನ್ನಡ (Kannada)",
        "en" to "English",
        "hi" to "हिन्दी (Hindi)",
        "te" to "తెలుగు (Telugu)",
        "ml" to "മലയാളം (Malayalam)",
        "ta" to "தமிழ் (Tamil)"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.select_language),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = bodyText
        )

        Spacer(modifier = Modifier.height(24.dp))

        languages.forEach { (code, name) ->
            Button(
                onClick = { onLanguageSelected(code) },
                colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(name, color = bodyText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.cancel), color = com.gandhasiri.app.ui.theme.MidBrown)
        }
    }
}
