package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcGold
import com.example.ui.theme.ArcSurfaceCard
import com.example.ui.theme.ArcSurfaceCardBorder
import com.example.ui.theme.ArcTextPrimary
import com.example.ui.theme.ArcTextSecondary

@Composable
fun SettingsDialog(
    pitch: Float,
    rate: Float,
    onPitchChange: (Float) -> Unit,
    onRateChange: (Float) -> Unit,
    isApiKeyConfigured: Boolean,
    onClearChatHistory: () -> Unit,
    onClearReminders: () -> Unit,
    onRequestPermission: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionsList = PermissionHelper.getPermissionStatusList(context)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ISRAEL AI SYSTEM SETTINGS",
                    color = ArcCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Permissions Matrix
                Text(
                    text = "PERMISSIONS & SERVICES STATUS",
                    color = ArcGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                permissionsList.forEach { perm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ArcSurfaceCardBorder, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = ArcSurfaceCardBorder.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (perm.isGranted) ArcCyan else Color(0xFFFF5252),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = perm.title,
                                        color = ArcTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = perm.description,
                                    color = ArcTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (perm.isGranted) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ArcCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "GRANTED",
                                        color = ArcCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        if (perm.actionIntent != null) {
                                            context.startActivity(perm.actionIntent)
                                        } else {
                                            onRequestPermission(perm.id)
                                        }
                                    },
                                    modifier = Modifier.height(28.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcGold)
                                ) {
                                    Text("GRANT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Section 2: Voice Synthesizer Controls
                Text(
                    text = "VOICE ENGINE CONTROLS",
                    color = ArcGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voice Pitch", color = ArcTextPrimary, fontSize = 12.sp)
                        Text(String.format("%.2f", pitch), color = ArcCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = pitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = ArcCyan, activeTrackColor = ArcCyan)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Speed", color = ArcTextPrimary, fontSize = 12.sp)
                        Text(String.format("%.2f", rate), color = ArcGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = rate,
                        onValueChange = onRateChange,
                        valueRange = 0.7f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = ArcGold, activeTrackColor = ArcGold)
                    )
                }

                // Section 3: Data Management
                Text(
                    text = "DATABASE & HISTORY",
                    color = ArcGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onClearChatHistory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcSurfaceCardBorder, contentColor = ArcTextPrimary)
                    ) {
                        Text("Clear History", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onClearReminders,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcSurfaceCardBorder, contentColor = ArcTextPrimary)
                    ) {
                        Text("Clear Alarms", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = ArcCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ArcSurfaceCard
    )
}
