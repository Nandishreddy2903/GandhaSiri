package com.gandhasiri.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandhasiri.app.viewmodel.TreesListViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    targetTreeId: String? = null,
    onTreeClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: TreesListViewModel = viewModel()
) {
    val trees by viewModel.trees.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    // Default camera position (Karnataka center)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(15.3173, 75.7139), 6f)
    }

    // Auto-zoom: if a specific tree is targeted, zoom straight to it; otherwise fit all trees
    LaunchedEffect(trees, targetTreeId) {
        if (trees.isEmpty()) return@LaunchedEffect
        if (targetTreeId != null) {
            val target = trees.firstOrNull { it.treeId == targetTreeId }
            if (target != null) {
                val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory
                    .newLatLngZoom(LatLng(target.latitude, target.longitude), 17f)
                cameraPositionState.animate(cameraUpdate)
            }
        } else {
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            trees.forEach { tree -> builder.include(LatLng(tree.latitude, tree.longitude)) }
            val bounds = builder.build()
            val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory
                .newLatLngBounds(bounds, 200)
            cameraPositionState.animate(cameraUpdate)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GandhaSiri - Map", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.gandhasiri.app.ui.theme.DarkWood
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                trees.forEachIndexed { index, tree ->
                    tree.treeId?.let { id ->
                        AnimatedMarker(
                            index = index,
                            position = LatLng(tree.latitude, tree.longitude),
                            title = "🌲 $id",
                            snippet = "Girth: ${tree.girthCm} cm. Tap for details.",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTreeClick(id)
                            }
                        )
                    }
                }
            }

            if (trees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.gandhasiri.app.ui.theme.Sandalwood)
                    ) {
                        Text(
                            "No trees registered yet. Tap the plus button to add your first tree.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = com.gandhasiri.app.ui.theme.NearBlackBrown,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMarker(
    index: Int,
    position: LatLng,
    title: String,
    snippet: String,
    onClick: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 120L) // Staggered drop
        startAnim = true
    }

    val animatedLat by animateFloatAsState(
        targetValue = if (startAnim) position.latitude.toFloat() else (position.latitude + 0.05).toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "lat"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(500), label = "alpha"
    )

    Marker(
        state = MarkerState(position = LatLng(animatedLat.toDouble(), position.longitude)),
        title = title,
        snippet = snippet,
        alpha = alpha,
        onInfoWindowClick = { _ -> onClick() }
    )
}

