package com.gandhasiri.app.ui.screens.addtree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gandhasiri.app.R

@Composable
fun ConfirmationScreen(treeId: String, onFinish: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = com.gandhasiri.app.ui.theme.WarmCream
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = com.gandhasiri.app.ui.theme.Sandalwood
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.registration_success),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = com.gandhasiri.app.ui.theme.NearBlackBrown,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.tree_recorded_msg),
                style = MaterialTheme.typography.bodyLarge,
                color = com.gandhasiri.app.ui.theme.MidBrown,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.tree_id_caps),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = com.gandhasiri.app.ui.theme.MidBrown
                    )
                    Text(
                        text = treeId,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = com.gandhasiri.app.ui.theme.Sandalwood
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.gandhasiri.app.ui.theme.Sandalwood
                )
            ) {
                Text(stringResource(R.string.return_to_dashboard), fontWeight = FontWeight.Bold)
            }
        }
    }
}
