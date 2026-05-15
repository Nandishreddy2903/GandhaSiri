package com.gandhasiri.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("gandhasiri_prefs", Context.MODE_PRIVATE)

    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var fullName by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var farmSize by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    val states = listOf("Karnataka", "Andhra Pradesh", "Tamil Nadu", "Kerala", "Maharashtra", "Telangana")
    var selectedState by remember { mutableStateOf(states[0]) }
    var stateDropdownExpanded by remember { mutableStateOf(false) }

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val background = com.gandhasiri.app.ui.theme.WarmCream
    val surface = com.gandhasiri.app.ui.theme.PureWhite
    val titleText = com.gandhasiri.app.ui.theme.NearBlackBrown
    val bodyText = com.gandhasiri.app.ui.theme.DarkWood
    val hintText = com.gandhasiri.app.ui.theme.MidBrown
    val accent = com.gandhasiri.app.ui.theme.Sandalwood
    val outline = com.gandhasiri.app.ui.theme.PaleWood

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profilePhotoUri = uri
    }

    Scaffold(
        containerColor = background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Create Your Profile",
                        fontWeight = FontWeight.Bold,
                        color = titleText
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo Picker
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(surface)
                    .border(2.dp, bodyText, CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profilePhotoUri != null) {
                    AsyncImage(
                        model = profilePhotoUri,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        modifier = Modifier.size(60.dp),
                        tint = outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = titleText,
                unfocusedTextColor = titleText,
                focusedBorderColor = accent,
                unfocusedBorderColor = outline,
                focusedLabelColor = bodyText,
                unfocusedLabelColor = hintText,
                cursorColor = accent
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = village,
                onValueChange = { village = it },
                label = { Text("Village / Town Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = district,
                onValueChange = { district = it },
                label = { Text("District") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // State Dropdown
            ExposedDropdownMenuBox(
                expanded = stateDropdownExpanded,
                onExpandedChange = { stateDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedState,
                    onValueChange = { },
                    label = { Text("State") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded)
                    },
                    colors = fieldColors
                )
                ExposedDropdownMenu(
                    expanded = stateDropdownExpanded,
                    onDismissRequest = { stateDropdownExpanded = false },
                    modifier = Modifier.background(surface)
                ) {
                    states.forEach { stateName ->
                        DropdownMenuItem(
                            text = { Text(stateName, color = bodyText) },
                            onClick = {
                                selectedState = stateName
                                stateDropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = farmSize,
                onValueChange = { farmSize = it },
                label = { Text("Farm Size in Acres") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 10) phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                prefix = { Text("+91 ", color = titleText) },
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Set Your PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = titleText
                )
                Text(
                    "Enter a 4 digit PIN to secure the app",
                    fontSize = 14.sp,
                    color = bodyText
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PinInputRow(
                    pinValue = pin,
                    onPinChange = { pin = it },
                    label = "New PIN",
                    textColor = bodyText
                )

                Spacer(modifier = Modifier.height(16.dp))

                PinInputRow(
                    pinValue = confirmPin,
                    onPinChange = { confirmPin = it },
                    label = "Confirm PIN",
                    textColor = bodyText
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (validateInputs(context, fullName, village, district, farmSize, phoneNumber, pin, confirmPin)) {
                        prefs.edit().apply {
                            putString("farmer_name", fullName)
                            putString("farmer_village", village)
                            putString("farmer_district", district)
                            putString("farmer_state", selectedState)
                            putString("farmer_phone", phoneNumber)
                            putString("farmer_farm_size", farmSize)
                            putString("farmer_photo_path", profilePhotoUri?.toString() ?: "")
                            putString("farmer_pin", pin)
                            putBoolean("pin_set", true)
                            apply()
                        }
                        onRegisterSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Complete Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PinInputRow(
    pinValue: String,
    onPinChange: (String) -> Unit,
    label: String,
    textColor: Color = Color.Gray
) {
    val focusRequester = remember { FocusRequester() }

    Column {
        Text(label, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier.clickable { focusRequester.requestFocus() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinValue.length
                    val digit = if (isFilled) "●" else ""
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, if (isFilled) com.gandhasiri.app.ui.theme.DarkWood else com.gandhasiri.app.ui.theme.PaleWood, RoundedCornerShape(8.dp))
                            .background(if (isFilled) com.gandhasiri.app.ui.theme.DarkWood else com.gandhasiri.app.ui.theme.PureWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isFilled) Color.White else textColor)
                    }
                }
            }
            
            // Hidden TextField to capture input
            androidx.compose.foundation.text.BasicTextField(
                value = pinValue,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        onPinChange(it)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester)
                    .alpha(0f)
            )
        }
    }
}

fun validateInputs(
    context: Context,
    name: String,
    village: String,
    district: String,
    farmSize: String,
    phone: String,
    pin: String,
    confirmPin: String
): Boolean {
    if (name.isBlank() || village.isBlank() || district.isBlank() || farmSize.isBlank() || phone.isBlank() || pin.isBlank() || confirmPin.isBlank()) {
        android.widget.Toast.makeText(context, "Please fill all fields", android.widget.Toast.LENGTH_SHORT).show()
        return false
    }
    if (phone.length != 10) {
        android.widget.Toast.makeText(context, "Phone number must be 10 digits", android.widget.Toast.LENGTH_SHORT).show()
        return false
    }
    if (pin.length != 4) {
        android.widget.Toast.makeText(context, "PIN must be 4 digits", android.widget.Toast.LENGTH_SHORT).show()
        return false
    }
    if (pin != confirmPin) {
        android.widget.Toast.makeText(context, "PINs do not match", android.widget.Toast.LENGTH_SHORT).show()
        return false
    }
    return true
}
