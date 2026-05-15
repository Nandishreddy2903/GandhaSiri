package com.gandhasiri.app.ui.screens

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandhasiri.app.data.entities.TreeMeasurement
import com.gandhasiri.app.ui.components.GrowthChartMP
import com.gandhasiri.app.viewmodel.TreeDetailViewModel
import com.gandhasiri.app.viewmodel.TreeDetailViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeasurementScreen(
    treeId: String,
    onBackClick: () -> Unit,
    viewModel: TreeDetailViewModel = viewModel(
        factory = TreeDetailViewModelFactory(
            LocalContext.current.applicationContext as Application,
            treeId
        )
    )
) {
    val haptic = LocalHapticFeedback.current
    val measurements by viewModel.measurements.collectAsState()
    val lastMeasurement = measurements.maxByOrNull { it.measuredAt }?.girthCm ?: 0.0

    var currentInput by remember { mutableStateOf(lastMeasurement) }
    var textInput by remember { mutableStateOf("") }
    
    val animatedGirth by animateFloatAsState(
        targetValue = currentInput.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "girthAnim"
    )

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Measurement", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.gandhasiri.app.ui.theme.DarkWood
                )
            )
        },
        containerColor = com.gandhasiri.app.ui.theme.WarmCream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Read-Only Growth Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.3f))
            ) {
                GrowthChartMP(
                    measurements = measurements,
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Circular Input Dial
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Minus Button
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentInput = maxOf(0.0, currentInput - 0.5) 
                        },
                    color = com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, com.gandhasiri.app.ui.theme.Sandalwood)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", fontSize = 32.sp, fontWeight = FontWeight.Black, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Central Display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(com.gandhasiri.app.ui.theme.Sandalwood)
                        .border(4.dp, com.gandhasiri.app.ui.theme.DarkWood, CircleShape)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f", animatedGirth),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "cm",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Plus Button
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentInput += 0.5 
                        },
                    color = com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, com.gandhasiri.app.ui.theme.Sandalwood)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Black, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Manual Text Input
            OutlinedTextField(
                value = textInput,
                onValueChange = { 
                    textInput = it
                    it.toDoubleOrNull()?.let { value -> currentInput = value }
                },
                label = { Text("Manual Entry") },
                placeholder = { Text("e.g. 45.5") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(180.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = com.gandhasiri.app.ui.theme.Sandalwood,
                    unfocusedBorderColor = com.gandhasiri.app.ui.theme.PaleWood,
                    focusedLabelColor = com.gandhasiri.app.ui.theme.Sandalwood,
                    unfocusedLabelColor = com.gandhasiri.app.ui.theme.MidBrown,
                    focusedTextColor = com.gandhasiri.app.ui.theme.NearBlackBrown,
                    unfocusedTextColor = com.gandhasiri.app.ui.theme.DarkWood
                ),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Before & After Preview
            val diff = currentInput - lastMeasurement
            val diffColor = if (diff > 0) com.gandhasiri.app.ui.theme.Sandalwood else if (diff < 0) com.gandhasiri.app.ui.theme.PanicRed else com.gandhasiri.app.ui.theme.MidBrown

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.gandhasiri.app.ui.theme.FaintWood)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PREVIOUS", fontSize = 11.sp, color = com.gandhasiri.app.ui.theme.MidBrown, fontWeight = FontWeight.Black)
                        Text("${String.format(Locale.US, "%.1f", lastMeasurement)} cm", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "To", tint = com.gandhasiri.app.ui.theme.Sandalwood)
                        Text(
                            text = (if (diff > 0) "+" else "") + String.format(Locale.US, "%.1f", diff),
                            color = diffColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEW", fontSize = 11.sp, color = com.gandhasiri.app.ui.theme.MidBrown, fontWeight = FontWeight.Black)
                        Text("${String.format(Locale.US, "%.1f", currentInput)} cm", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = com.gandhasiri.app.ui.theme.Sandalwood)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        viewModel.saveNewMeasurement(currentInput)
                        delay(500)
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.gandhasiri.app.ui.theme.Sandalwood,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("SAVE MEASUREMENT", fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
