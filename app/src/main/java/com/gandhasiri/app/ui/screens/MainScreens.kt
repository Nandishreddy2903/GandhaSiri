package com.gandhasiri.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gandhasiri.app.data.entities.Tree
import com.gandhasiri.app.viewmodel.TreesListViewModel
import androidx.compose.ui.res.stringResource
import com.gandhasiri.app.R
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreesScreen(
    onAddTreeClick: () -> Unit,
    onTreeClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: TreesListViewModel = viewModel()
) {
    val trees by viewModel.trees.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Breathing FAB Animation
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val fabScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (trees.isEmpty()) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GandhaSiri", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.gandhasiri.app.ui.theme.DarkWood,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.profile), tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddTreeClick() 
                },
                modifier = Modifier.scale(fabScale),
                containerColor = com.gandhasiri.app.ui.theme.Sandalwood,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_tree))
            }
        },
        containerColor = com.gandhasiri.app.ui.theme.WarmCream
    ) { innerPadding ->
        if (trees.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_trees_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = com.gandhasiri.app.ui.theme.DarkWood,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                itemsIndexed(trees) { index, tree ->
                    StaggeredTreeItem(
                        index = index,
                        tree = tree,
                        onClick = { tree.treeId?.let { onTreeClick(it) } }
                    )
                }
            }
        }
    }
}

@Composable
fun StaggeredTreeItem(index: Int, tree: Tree, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(400)),
        exit = fadeOut()
    ) {
        SwipeableTreeItem(tree = tree, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTreeItem(tree: Tree, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                tree.treeId?.let { id -> onClick() }
                false // Don't actually remove from list
            } else false
        },
        positionalThreshold = { distance -> distance * 0.8f } // High threshold for "complete swipe"
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.padding(vertical = 4.dp),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f)
            } else Color.Transparent
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.view_details), fontWeight = FontWeight.Bold, color = com.gandhasiri.app.ui.theme.DarkWood)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = com.gandhasiri.app.ui.theme.DarkWood)
                    }
                }
            }
        }
    ) {
        TreeListItem(tree = tree, onClick = onClick)
    }
}

@Composable
fun TreeListItem(tree: Tree, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick() 
            }
            .border(1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite)
    ) {
        Box {
            // Left Border Accent Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(com.gandhasiri.app.ui.theme.Sandalwood)
            )

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(start = 8.dp) // Offset for accent line
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = File(tree.photoPath),
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = tree.treeId ?: stringResource(R.string.generating),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = com.gandhasiri.app.ui.theme.NearBlackBrown
                    )
                    Text(
                        text = stringResource(R.string.girth_age_format, tree.girthCm, tree.ageYears),
                        fontSize = 14.sp,
                        color = com.gandhasiri.app.ui.theme.DarkWood
                    )
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tree.createdAt)),
                        fontSize = 12.sp,
                        color = com.gandhasiri.app.ui.theme.MidBrown
                    )
                }
            }
        }
    }
}



