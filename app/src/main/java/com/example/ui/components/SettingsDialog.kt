package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
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
    wakeWordEnabled: Boolean,
    onWakeWordToggle: (Boolean) -> Unit,
    isApiKeyConfigured: Boolean,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ISRAEL AI SYSTEM CONFIG",
                color = ArcCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wake word option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "'Hey Israel' Wake Word",
                            color = ArcTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Listens for 'Hey Israel' trigger keyword",
                            color = ArcTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = onWakeWordToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArcCyan,
                            checkedTrackColor = ArcCyan.copy(alpha = 0.3f)
                        )
                    )
                }

                // Voice Pitch
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Voice Pitch (JARVIS Tone)",
                            color = ArcTextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format("%.2f", pitch),
                            color = ArcCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = pitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = ArcCyan,
                            activeTrackColor = ArcCyan
                        )
                    )
                }

                // Voice Speed
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speech Delivery Speed",
                            color = ArcTextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format("%.2f", rate),
                            color = ArcGold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = rate,
                        onValueChange = onRateChange,
                        valueRange = 0.7f..1.6f,
                        colors = SliderDefaults.colors(
                            thumbColor = ArcGold,
                            activeTrackColor = ArcGold
                        )
                    )
                }

                // Gemini API Status Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ArcSurfaceCardBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isApiKeyConfigured) ArcCyan.copy(alpha = 0.1f) else ArcGold.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isApiKeyConfigured) "● Gemini AI Engine Connected" else "▲ Gemini Free Tier Active",
                            color = if (isApiKeyConfigured) ArcCyan else ArcGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isApiKeyConfigured)
                                "Using Gemini 3.5 Flash via standard API key configuration."
                            else
                                "Provide GEMINI_API_KEY in AI Studio Secrets panel for enhanced natural language parsing.",
                            color = ArcTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Clear Agenda
                Button(
                    onClick = onClearAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcSurfaceCardBorder,
                        contentColor = ArcTextPrimary
                    )
                ) {
                    Text("Clear All Israel Reminders", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = ArcCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ArcSurfaceCard
    )
}
