package com.gandhasiri.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.gandhasiri.app.R
import coil.compose.AsyncImage
import com.gandhasiri.app.viewmodel.TreeDetailViewModel
import com.gandhasiri.app.viewmodel.TreeDetailViewModelFactory
import java.io.File

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import com.gandhasiri.app.data.entities.TreeMeasurement
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeDetailScreen(
    treeId: String,
    onBackClick: () -> Unit,
    onAddMeasurementClick: () -> Unit,
    onViewOnMap: (String) -> Unit,
    viewModel: TreeDetailViewModel = viewModel(
        factory = TreeDetailViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            treeId
        )
    )
) {
    val tree by viewModel.tree.collectAsState()
    val measurements by viewModel.measurements.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showEditAgeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddMeasurementSheet by remember { mutableStateOf(false) }
    
    // Close sheet automatically when saving completes successfully
    LaunchedEffect(isSaving) {
        if (!isSaving && showAddMeasurementSheet) {
            showAddMeasurementSheet = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.tree_details), 
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back), 
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.options),
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(com.gandhasiri.app.ui.theme.PureWhite)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_age), color = com.gandhasiri.app.ui.theme.DarkWood, fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                showEditAgeDialog = true
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = com.gandhasiri.app.ui.theme.Sandalwood) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = com.gandhasiri.app.ui.theme.PanicRed, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = com.gandhasiri.app.ui.theme.PanicRed) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.gandhasiri.app.ui.theme.DarkWood,
                    scrolledContainerColor = com.gandhasiri.app.ui.theme.DarkWood
                )
            )
        },
        containerColor = com.gandhasiri.app.ui.theme.WarmCream
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val tabs = listOf(stringResource(R.string.overview_tab), stringResource(R.string.security_log_tab))
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = com.gandhasiri.app.ui.theme.DarkWood,
                contentColor = com.gandhasiri.app.ui.theme.LightGold,
                divider = { },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTabIndex])
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        height = 4.dp,
                        color = com.gandhasiri.app.ui.theme.LightGold
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedTabIndex == index
                    Tab(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                text = title, 
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp,
                                color = if (selected) com.gandhasiri.app.ui.theme.LightGold else com.gandhasiri.app.ui.theme.LightGold.copy(alpha = 0.6f)
                            ) 
                        }
                    )
                }
            }

            tree?.let { t ->
                when (selectedTabIndex) {
                    0 -> {
                        val scrollState = rememberScrollState()
                        TreeDetailsTab(
                            tree = t,
                            measurements = measurements,
                            viewModel = viewModel,
                            onAddMeasurementClick = { showAddMeasurementSheet = true },
                            onViewOnMap = { onViewOnMap(t.treeId ?: "") },
                            scrollState = scrollState
                        )
                    }
                    1 -> {
                        TreeSecurityTab(treeId = treeId, viewModel = viewModel)
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator(
                        color = com.gandhasiri.app.ui.theme.Sandalwood,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }

    if (showEditAgeDialog) {
        var ageInput by remember { mutableStateOf(tree?.ageYears?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showEditAgeDialog = false },
            title = {
                Text(stringResource(R.string.edit_tree_age), fontWeight = FontWeight.Bold, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
            },
            text = {
                OutlinedTextField(
                    value = ageInput,
                    onValueChange = { ageInput = it },
                    label = { Text(stringResource(R.string.age_years_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.gandhasiri.app.ui.theme.Sandalwood,
                        unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ageInput.toIntOrNull()?.let { newAge ->
                        viewModel.updateAge(newAge)
                    }
                    showEditAgeDialog = false
                }) {
                    Text(stringResource(R.string.save), color = com.gandhasiri.app.ui.theme.Sandalwood, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAgeDialog = false }) {
                    Text(stringResource(R.string.cancel), color = com.gandhasiri.app.ui.theme.MidBrown)
                }
            },
            containerColor = com.gandhasiri.app.ui.theme.PureWhite
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(stringResource(R.string.delete_tree_confirm), fontWeight = FontWeight.Bold, color = com.gandhasiri.app.ui.theme.PanicRed)
            },
            text = {
                Text(stringResource(R.string.delete_tree_msg), color = com.gandhasiri.app.ui.theme.DarkWood)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTree(onDeleted = onBackClick)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.PanicRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("DELETE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = com.gandhasiri.app.ui.theme.MidBrown)
                }
            },
            containerColor = com.gandhasiri.app.ui.theme.PureWhite
        )
    }

    if (showAddMeasurementSheet) {
        val sheetState = rememberModalBottomSheetState()
        var girthInput by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { if (!isSaving) showAddMeasurementSheet = false },
            sheetState = sheetState,
            containerColor = com.gandhasiri.app.ui.theme.WarmCream
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Add New Measurement",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.gandhasiri.app.ui.theme.NearBlackBrown
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = girthInput,
                    onValueChange = { girthInput = it },
                    label = { Text("Current Girth (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.gandhasiri.app.ui.theme.Sandalwood,
                        unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        girthInput.toDoubleOrNull()?.let {
                            viewModel.saveNewMeasurement(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSaving && girthInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.Sandalwood),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("SAVE MEASUREMENT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GrowthChart(measurements: List<TreeMeasurement>) {
    val darkWood = com.gandhasiri.app.ui.theme.DarkWood.toArgb()
    val sandalwood = com.gandhasiri.app.ui.theme.Sandalwood.toArgb()

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.textColor = darkWood
                xAxis.valueFormatter = object : ValueFormatter() {
                    private val mFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return mFormat.format(Date(value.toLong()))
                    }
                }
                axisRight.isEnabled = false
                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor = darkWood
                axisLeft.textColor = darkWood
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = measurements.map { 
                Entry(it.measuredAt.toFloat(), it.girthCm.toFloat()) 
            }
            val dataSet = LineDataSet(entries, "Girth (cm)").apply {
                color = sandalwood
                setCircleColor(darkWood)
                lineWidth = 3f
                circleRadius = 5f
                setDrawCircleHole(true)
                circleHoleColor = Color.White.toArgb()
                valueTextSize = 10f
                setDrawFilled(true)
                fillColor = sandalwood
                fillAlpha = 30
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
            }
            chart.data = LineData(dataSet)
            chart.animateX(1000)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun MeasurementListItem(measurement: TreeMeasurement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(com.gandhasiri.app.ui.theme.PureWhite, RoundedCornerShape(8.dp))
            .border(1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(measurement.measuredAt)),
            style = MaterialTheme.typography.bodyMedium,
            color = com.gandhasiri.app.ui.theme.DarkWood
        )
        Text(
            text = "${measurement.girthCm} cm",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = com.gandhasiri.app.ui.theme.Sandalwood
        )
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = com.gandhasiri.app.ui.theme.Sandalwood)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = com.gandhasiri.app.ui.theme.DarkWood)
    }
}
