package com.gandhasiri.app.ui.screens.addtree

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.gandhasiri.app.viewmodel.AddTreeViewModel
import androidx.compose.ui.res.stringResource
import com.gandhasiri.app.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun AddTreeScreen(
    onTreeSaved: (String) -> Unit,
    viewModel: AddTreeViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission  = perms[Manifest.permission.CAMERA] == true
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val photoPath by viewModel.photoPath.collectAsState()

    if (hasCameraPermission && hasLocationPermission) {
        if (photoPath == null) {
            CameraView(
                onImageCaptured = { uri -> viewModel.setPhotoPath(uri.path ?: "") },
                onError = {}
            )
        } else {
            AddTreeForm(viewModel, onTreeSaved, coroutineScope)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(
                    "Camera and Location permissions are required.",
                    fontWeight = FontWeight.Bold,
                    color = com.gandhasiri.app.ui.theme.NearBlackBrown
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) { Text("Grant Permissions") }
            }
        }
    }
}

@Composable
fun AddTreeForm(viewModel: AddTreeViewModel, onTreeSaved: (String) -> Unit, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val latitude  by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()

    // Local GPS status — drives the field label only; does not block saving
    var locationFetching by remember { mutableStateOf(true) }
    var locationFailed   by remember { mutableStateOf(false) }

    var girthCm  by remember { mutableStateOf("") }
    var ageYears by remember { mutableStateOf("") }
    var notes    by remember { mutableStateOf("") }

    // ── Location fetch ───────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        val hasFine = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            locationFetching = false
            locationFailed   = true
            return@LaunchedEffect
        }

        val fused = LocationServices.getFusedLocationProviderClient(context)

        // Step 1: getLastLocation — instant if OS has a recent fix
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    Log.d("GandhaSiri", "AddTree lastLocation: ${loc.latitude}, ${loc.longitude}")
                    viewModel.setLocation(loc.latitude, loc.longitude)
                    locationFetching = false
                } else {
                    Log.w("GandhaSiri", "AddTree lastLocation null — requesting fresh fix")

                    // Step 2: Request a fresh fix using requestLocationUpdates with timeout
                    coroutineScope.launch(Dispatchers.Main) {
                        val freshLocation = withTimeoutOrNull(10_000L) {
                            suspendCancellableCoroutine<android.location.Location?> { cont ->
                                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                                    Priority.PRIORITY_HIGH_ACCURACY, 1000L
                                ).setMaxUpdates(1).build()

                                val callback = object : com.google.android.gms.location.LocationCallback() {
                                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                                        val resultLoc = result.lastLocation
                                        if (resultLoc != null) {
                                            fused.removeLocationUpdates(this)
                                            if (cont.isActive) cont.resume(resultLoc)
                                        }
                                    }
                                }

                                try {
                                    fused.requestLocationUpdates(locationRequest, callback, android.os.Looper.getMainLooper())
                                    cont.invokeOnCancellation {
                                        fused.removeLocationUpdates(callback)
                                    }
                                } catch (e: SecurityException) {
                                    Log.e("GandhaSiri", "AddTree SecurityException: ${e.message}")
                                    if (cont.isActive) cont.resume(null)
                                }
                            }
                        }

                        if (freshLocation != null) {
                            Log.d("GandhaSiri", "AddTree requestLocationUpdates: ${freshLocation.latitude}, ${freshLocation.longitude}")
                            viewModel.setLocation(freshLocation.latitude, freshLocation.longitude)
                        } else {
                            Log.e("GandhaSiri", "AddTree: requestLocationUpdates returned null or timed out")
                            locationFailed = true
                        }
                        locationFetching = false
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GandhaSiri", "AddTree lastLocation failed: ${e.message}")
                locationFailed   = true
                locationFetching = false
            }
    }
    // ────────────────────────────────────────────────────────────────────────

    val photoPath by viewModel.photoPath.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.gandhasiri.app.ui.theme.WarmCream)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (photoPath != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(photoPath!!)),
                    contentDescription = stringResource(R.string.sandalwood_specimen),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Text(
            text = stringResource(R.string.tree_registration),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = com.gandhasiri.app.ui.theme.NearBlackBrown,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp)
        )

        // ── GPS Coordinates field ────────────────────────────────────────────
        val coordLabel = when {
            locationFetching          -> stringResource(R.string.fetching_gps)
            locationFailed            -> stringResource(R.string.gps_failed_retry)
            else                      -> stringResource(R.string.gps_coordinates)
        }
        val coordValue = when {
            locationFetching          -> ""
            locationFailed && latitude == 0.0 -> ""
            else                      -> "%.6f, %.6f".format(latitude, longitude)
        }
        val borderColor = when {
            locationFailed -> com.gandhasiri.app.ui.theme.PanicRed
            latitude != 0.0 -> com.gandhasiri.app.ui.theme.Sandalwood
            else -> com.gandhasiri.app.ui.theme.PaleWood
        }

        OutlinedTextField(
            value = coordValue,
            onValueChange = {},
            label = { Text(coordLabel) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = borderColor,
                unfocusedBorderColor = borderColor,
                focusedTextColor     = com.gandhasiri.app.ui.theme.NearBlackBrown,
                unfocusedTextColor   = com.gandhasiri.app.ui.theme.NearBlackBrown,
                focusedLabelColor    = borderColor,
                unfocusedLabelColor  = borderColor
            ),
            trailingIcon = {
                if (locationFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = com.gandhasiri.app.ui.theme.Sandalwood
                    )
                }
            }
        )

        // Retry GPS row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (locationFailed) {
                Text(
                    stringResource(R.string.location_not_available),
                    fontSize = 11.sp,
                    color = com.gandhasiri.app.ui.theme.PanicRed
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (locationFailed || (!locationFetching && latitude == 0.0)) {
                TextButton(onClick = {
                    locationFetching = true
                    locationFailed   = false
                    val fused = LocationServices.getFusedLocationProviderClient(context)
                    coroutineScope.launch(Dispatchers.Main) {
                        val freshLocation = withTimeoutOrNull(10_000L) {
                            suspendCancellableCoroutine<android.location.Location?> { cont ->
                                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                                    Priority.PRIORITY_HIGH_ACCURACY, 1000L
                                ).setMaxUpdates(1).build()

                                val callback = object : com.google.android.gms.location.LocationCallback() {
                                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                                        val resultLoc = result.lastLocation
                                        if (resultLoc != null) {
                                            fused.removeLocationUpdates(this)
                                            if (cont.isActive) cont.resume(resultLoc)
                                        }
                                    }
                                }

                                try {
                                    fused.requestLocationUpdates(locationRequest, callback, android.os.Looper.getMainLooper())
                                    cont.invokeOnCancellation {
                                        fused.removeLocationUpdates(callback)
                                    }
                                } catch (e: SecurityException) {
                                    Log.e("GandhaSiri", "AddTree Retry SecurityException: ${e.message}")
                                    if (cont.isActive) cont.resume(null)
                                }
                            }
                        }

                        if (freshLocation != null) {
                            viewModel.setLocation(freshLocation.latitude, freshLocation.longitude)
                        } else {
                            locationFailed = true
                        }
                        locationFetching = false
                    }
                }) {
                    Text(
                        stringResource(R.string.retry_gps),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.gandhasiri.app.ui.theme.Sandalwood
                    )
                }
            }
        }
        // ────────────────────────────────────────────────────────────────────

        OutlinedTextField(
            value = girthCm,
            onValueChange = { girthCm = it },
            label = { Text(stringResource(R.string.girth_cm_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = com.gandhasiri.app.ui.theme.Sandalwood,
                unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood,
                focusedLabelColor    = com.gandhasiri.app.ui.theme.Sandalwood,
                unfocusedLabelColor  = com.gandhasiri.app.ui.theme.DarkWood,
                focusedTextColor     = com.gandhasiri.app.ui.theme.NearBlackBrown,
                unfocusedTextColor   = com.gandhasiri.app.ui.theme.NearBlackBrown
            )
        )

        OutlinedTextField(
            value = ageYears,
            onValueChange = { ageYears = it },
            label = { Text(stringResource(R.string.estimated_age_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = com.gandhasiri.app.ui.theme.Sandalwood,
                unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood,
                focusedLabelColor    = com.gandhasiri.app.ui.theme.Sandalwood,
                unfocusedLabelColor  = com.gandhasiri.app.ui.theme.DarkWood,
                focusedTextColor     = com.gandhasiri.app.ui.theme.NearBlackBrown,
                unfocusedTextColor   = com.gandhasiri.app.ui.theme.NearBlackBrown
            )
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.additional_notes_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = com.gandhasiri.app.ui.theme.Sandalwood,
                unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood,
                focusedLabelColor    = com.gandhasiri.app.ui.theme.Sandalwood,
                unfocusedLabelColor  = com.gandhasiri.app.ui.theme.DarkWood,
                focusedTextColor     = com.gandhasiri.app.ui.theme.NearBlackBrown,
                unfocusedTextColor   = com.gandhasiri.app.ui.theme.NearBlackBrown
            )
        )

        val isSaveEnabled = girthCm.isNotBlank() && 
                           girthCm.toDoubleOrNull() != null && 
                           ageYears.isNotBlank() && 
                           ageYears.toIntOrNull() != null

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val g = girthCm.toDoubleOrNull()  ?: 0.0
                val a = ageYears.toIntOrNull()     ?: 0
                viewModel.saveTree(g, a, notes) { generatedId -> onTreeSaved(generatedId) }
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = com.gandhasiri.app.ui.theme.Sandalwood,
                contentColor   = Color.White,
                disabledContainerColor = com.gandhasiri.app.ui.theme.PaleWood,
                disabledContentColor = com.gandhasiri.app.ui.theme.MidBrown
            )
        ) {
            Text(stringResource(R.string.save_tree_record), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
