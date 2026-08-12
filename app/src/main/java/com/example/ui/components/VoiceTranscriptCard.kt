package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.voice.IsraelSpeechState

@Composable
fun VoiceTranscriptCard(
    speechState: IsraelSpeechState,
    userTranscript: String,
    israelResponse: String,
    onReplaySpeech: () -> Unit,
    onStopSpeech: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = when (speechState) {
        IsraelSpeechState.IDLE -> "ISRAEL CORE ONLINE"
        IsraelSpeechState.LISTENING -> "LISTENING TO VOICE..."
        IsraelSpeechState.THINKING -> "PROCESSING COMMAND VIA GEMINI..."
        IsraelSpeechState.SPEAKING -> "TRANSMITTING VOICE RESPONSE..."
        IsraelSpeechState.ERROR -> "SYSTEM NOTICE"
    }

    val statusColor = when (speechState) {
        IsraelSpeechState.LISTENING -> ArcGold
        IsraelSpeechState.SPEAKING -> ArcCyan
        IsraelSpeechState.THINKING -> ArcCyanGlow
        else -> ArcCyan
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_transcript_card")
            .border(1.dp, ArcSurfaceCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = ArcSurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status HUD bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                if (speechState == IsraelSpeechState.SPEAKING) {
                    IconButton(
                        onClick = onStopSpeech,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("stop_speech_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Speech",
                            tint = ArcGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (israelResponse.isNotBlank()) {
                    IconButton(
                        onClick = onReplaySpeech,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("replay_speech_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Replay Speech",
                            tint = ArcCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Speech transcript box
            if (userTranscript.isNotBlank()) {
                Text(
                    text = "COMMAND:",
                    color = ArcTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\"$userTranscript\"",
                    color = ArcTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Israel Response box
            Text(
                text = "ISRAEL:",
                color = ArcCyanGlow,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = israelResponse.ifEmpty { "Say \"Hey Israel\" or tap the reactor core to give a voice command." },
                color = if (israelResponse.isEmpty()) ArcTextSecondary else ArcCyanGlow,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
