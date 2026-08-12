package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.IsraelViewModel
import com.example.ui.components.AgendaPanel
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.QuickActionChips
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VoiceTranscriptCard
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.ArcDeepBackground
import com.example.ui.theme.ArcGold
import com.example.ui.theme.ArcSurfaceCard
import com.example.ui.theme.ArcTextPrimary
import com.example.ui.theme.ArcTextSecondary
import com.example.ui.theme.IsraelTheme

class MainActivity : ComponentActivity() {

    private val viewModel: IsraelViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordGranted) {
            Toast.makeText(this, "Microphone permission required for Israel Voice Assistant", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermissions()

        setContent {
            IsraelTheme {
                IsraelMainScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsraelMainScreen(viewModel: IsraelViewModel) {
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()
    val rmsDb by viewModel.rmsDb.collectAsStateWithLifecycle()
    val userTranscript by viewModel.userTranscript.collectAsStateWithLifecycle()
    val israelResponse by viewModel.israelResponse.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val pitch by viewModel.pitch.collectAsStateWithLifecycle()
    val rate by viewModel.rate.collectAsStateWithLifecycle()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val apiKeyConfigured = try {
        val key = BuildConfig.GEMINI_API_KEY
        key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "null"
    } catch (e: Exception) {
        false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcDeepBackground),
        containerColor = ArcDeepBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ArcCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Israel AI",
                                tint = ArcCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ISRAEL AI",
                                color = ArcCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Jarvis Protocol • Google Pixel Core",
                                color = ArcTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ArcCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArcDeepBackground,
                    titleContentColor = ArcCyan
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Central Glowing Arc Reactor Core
            ArcReactorVisualizer(
                speechState = speechState,
                rmsDb = rmsDb,
                onClick = { viewModel.onReactorClick() },
                size = 230.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tap hint button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ArcSurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Tap to speak",
                        tint = ArcGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (speechState == com.example.voice.IsraelSpeechState.LISTENING) "Listening... Speak command" else "Tap Core or say 'Hey Israel'",
                        color = ArcTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Command & Response Transcript HUD
            VoiceTranscriptCard(
                speechState = speechState,
                userTranscript = userTranscript,
                israelResponse = israelResponse,
                onReplaySpeech = { viewModel.speakText(israelResponse) },
                onStopSpeech = { viewModel.stopSpeaking() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Voice Action Chips
            QuickActionChips(
                onActionClick = { command -> viewModel.processVoiceCommand(command) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Israel Schedule & Appointments Agenda Panel
            AgendaPanel(
                reminders = reminders,
                onToggleComplete = { reminder -> viewModel.toggleReminderComplete(reminder) },
                onDeleteReminder = { id -> viewModel.deleteReminder(id) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isSettingsOpen) {
            SettingsDialog(
                pitch = pitch,
                rate = rate,
                onPitchChange = { viewModel.updatePitch(it) },
                onRateChange = { viewModel.updateRate(it) },
                wakeWordEnabled = wakeWordEnabled,
                onWakeWordToggle = { viewModel.toggleWakeWord(it) },
                isApiKeyConfigured = apiKeyConfigured,
                onClearAll = { viewModel.clearAllReminders() },
                onDismiss = { viewModel.closeSettings() }
            )
        }
    }
}
