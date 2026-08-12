package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.ArcGold
import com.example.ui.theme.ArcSurfaceCard
import com.example.ui.theme.ArcSurfaceCardBorder
import com.example.ui.theme.ArcTextPrimary
import com.example.ui.theme.ArcTextSecondary
import com.example.voice.IsraelSpeechState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceTranscriptCard(
    speechState: IsraelSpeechState,
    userTranscript: String,
    israelResponse: String,
    chatHistory: List<ChatMessageEntity>,
    onReplaySpeech: () -> Unit,
    onStopSpeech: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatHistory.size, userTranscript, israelResponse) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val statusText = when (speechState) {
        IsraelSpeechState.IDLE -> "ISRAEL CORE ONLINE"
        IsraelSpeechState.LISTENING -> "LISTENING TO VOICE..."
        IsraelSpeechState.THINKING -> "PROCESSING COMMAND..."
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

            // Conversation Chat Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chatHistory.isEmpty() && userTranscript.isBlank() && israelResponse.isBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No conversation history. Tap mic orb or speak to start.",
                                color = ArcTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                items(chatHistory) { msg ->
                    ChatBubble(msg)
                }

                // Live partial transcript item if currently listening/thinking
                if (speechState == IsraelSpeechState.LISTENING && userTranscript.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ArcGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Listening: \"$userTranscript\"",
                                    color = ArcGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessageEntity) {
    val isUser = msg.sender == "USER"
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isUser) "YOU" else "ISRAEL",
                    color = if (isUser) ArcGold else ArcCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeStr,
                    color = ArcTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(if (isUser) ArcSurfaceCardBorder else ArcCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = msg.text,
                        color = if (isUser) ArcTextPrimary else ArcCyanGlow,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    if (!msg.actionType.isNullOrBlank() && msg.actionType != "GENERAL_CHAT") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ArcGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Action: ${msg.actionType}",
                                color = ArcGold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
