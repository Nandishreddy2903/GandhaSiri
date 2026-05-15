package com.gandhasiri.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.gandhasiri.app.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gandhasiri.app.data.entities.Tree
import com.gandhasiri.app.data.entities.TreeMeasurement
import com.gandhasiri.app.ui.components.GrowthChartMP
import com.gandhasiri.app.ui.components.GrowthJourneyCard
import com.gandhasiri.app.viewmodel.TreeDetailViewModel
import kotlinx.coroutines.delay
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun TreeDetailsTab(
    tree: Tree,
    measurements: List<TreeMeasurement>,
    viewModel: TreeDetailViewModel,
    onAddMeasurementClick: () -> Unit,
    onViewOnMap: () -> Unit,
    scrollState: ScrollState
) {
    val haptic = LocalHapticFeedback.current

    // Trigger for entrance animations
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Save the URI to DB. Note: For a real app we might need to copy the file or take persistable permissions.
            // But since this is a quick update, saving the URI string directly.
            viewModel.updatePhoto(it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp)
            .background(com.gandhasiri.app.ui.theme.WarmCream)
    ) {
        // 1. Premium Parallax Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        ) {
            AsyncImage(
                model = File(tree.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = scrollState.value * 0.4f // Smoother parallax
                        alpha = 1f - (scrollState.value / 800f).coerceIn(0f, 0.5f) // Fade on scroll
                    }
            )
            // Elegant Multi-stop Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                com.gandhasiri.app.ui.theme.DarkWood.copy(alpha = 0.3f),
                                com.gandhasiri.app.ui.theme.DarkWood.copy(alpha = 0.8f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Update Photo Button Overlay
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 24.dp, start = 24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Update Photo",
                    tint = Color.White
                )
            }

            // Glassmorphic Tree ID Badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(com.gandhasiri.app.ui.theme.Sandalwood)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tree.treeId ?: stringResource(R.string.generating),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.sandalwood_specimen),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.health_overview),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Animated Stats Row
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500))
        ) {
            AnimatedStatsRow(tree = tree)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2b. Location Info Card + View on Map Button
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { 60 }) + fadeIn(tween(550, delayMillis = 60))
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = com.gandhasiri.app.ui.theme.Sandalwood,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.gps_location),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = com.gandhasiri.app.ui.theme.MidBrown,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%.6f° N".format(tree.latitude),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.gandhasiri.app.ui.theme.NearBlackBrown
                            )
                            Text(
                                text = "%.6f° E".format(tree.longitude),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.gandhasiri.app.ui.theme.NearBlackBrown
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onViewOnMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gandhasiri.app.ui.theme.DarkWood,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.view_on_map_btn),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3. Growth Journey Card (Actual + AI Projection dual-line chart)
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(600, delayMillis = 100))
        ) {
            val projectedGrowth by viewModel.projectedGrowth.collectAsState()
            val isLoadingProjection by viewModel.isLoadingProjection.collectAsState()
            val projectionError by viewModel.projectionError.collectAsState()

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timeline,
                        contentDescription = null,
                        tint = com.gandhasiri.app.ui.theme.Sandalwood,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Growth Journey",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = com.gandhasiri.app.ui.theme.NearBlackBrown
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                GrowthJourneyCard(
                    measurements = measurements,
                    projectedGrowth = projectedGrowth,
                    isLoadingProjection = isLoadingProjection,
                    projectionError = projectionError,
                    onRegenerate = {
                        val t = tree
                        val plantingYear = java.util.Calendar.getInstance()
                            .apply { timeInMillis = t.createdAt }.get(java.util.Calendar.YEAR)
                        viewModel.fetchGrowthProjection(
                            currentGirthCm = t.girthCm,
                            ageYears = t.ageYears,
                            plantingYear = plantingYear,
                            measurementHistory = measurements,
                            forceRefresh = true
                        )
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Add Measurement Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onAddMeasurementClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.gandhasiri.app.ui.theme.Sandalwood,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ADD MEASUREMENT",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // AI Harvest Insight Card
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    AIEstimateCard(tree = tree, viewModel = viewModel)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


@Composable
fun AnimatedStatsRow(tree: Tree) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val daysRegistered = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - tree.createdAt)

        StatCard(
            label = "Current Girth",
            value = "${tree.girthCm} cm",
            icon = Icons.Rounded.MonitorWeight,
            color = com.gandhasiri.app.ui.theme.Sandalwood,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Est. Age",
            value = "${tree.ageYears} yrs",
            icon = Icons.Rounded.DateRange,
            color = com.gandhasiri.app.ui.theme.MidBrown,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = com.gandhasiri.app.ui.theme.NearBlackBrown
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = com.gandhasiri.app.ui.theme.DarkWood,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AIEstimateCard(tree: Tree, viewModel: TreeDetailViewModel) {
    val haptic = LocalHapticFeedback.current
    val isLoadingAI by viewModel.isLoadingAI.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    // Subtle animated gradient border for AI card
    val infiniteTransition = rememberInfiniteTransition(label = "aiBorder")
    val borderOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "borderOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            Brush.linearGradient(
                colors = listOf(
                    com.gandhasiri.app.ui.theme.MidBrown.copy(alpha = 0.3f),
                    com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.6f),
                    com.gandhasiri.app.ui.theme.MidBrown.copy(alpha = 0.3f)
                ),
                start = Offset(borderOffset, borderOffset),
                end = Offset(borderOffset + 300f, borderOffset + 300f)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Very subtle AI background tint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                com.gandhasiri.app.ui.theme.WarmCream.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 600f
                        )
                    )
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI Maturity Insight",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = com.gandhasiri.app.ui.theme.NearBlackBrown
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.getAIEstimate(tree.ageYears, tree.girthCm)
                        },
                        enabled = !isLoadingAI,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(com.gandhasiri.app.ui.theme.WarmCream.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Regenerate",
                            tint = com.gandhasiri.app.ui.theme.Sandalwood,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoadingAI) {
                    ShimmerEffect()
                } else if (aiError != null) {
                    Text(
                        text = aiError ?: "Error generating insight.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (tree.aiEstimate != null) {
                    val parts = tree.aiEstimate.split("\n", limit = 2)
                    val headline = parts.getOrNull(0) ?: ""
                    val rest = parts.getOrNull(1) ?: ""

                    Text(
                        text = headline.replace("**", ""),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = com.gandhasiri.app.ui.theme.Sandalwood,
                        lineHeight = 28.sp
                    )

                    if (rest.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = rest.replace("**", ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.gandhasiri.app.ui.theme.DarkWood.copy(alpha = 0.8f),
                            lineHeight = 22.sp
                        )
                    }

                    if (tree.aiTimestamp != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(com.gandhasiri.app.ui.theme.MidBrown))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generated: ${dateFormat.format(Date(tree.aiTimestamp))}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = com.gandhasiri.app.ui.theme.MidBrown
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Tap the refresh icon to generate an advanced AI insight on this specimen's expected heartwood yield.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.gandhasiri.app.ui.theme.MidBrown,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AIPromptBanner(onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBEE), // Light amber
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFECB3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update Available",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF57C00)
                )
                Text(
                    text = "New measurements detected. Tap to refresh the AI estimate.",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ShimmerEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth(0.9f).height(28.dp).clip(RoundedCornerShape(8.dp)).background(brush))
        Box(modifier = Modifier.fillMaxWidth(0.95f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        Box(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(brush))
    }
}
