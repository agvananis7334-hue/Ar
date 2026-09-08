package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantMessage
import com.example.data.model.AssistantStatus
import com.example.data.model.LanguageMode
import com.example.data.model.MessageSender
import com.example.data.model.QuickSuggestion
import com.example.ui.components.ScreenOffSimulationView
import com.example.ui.components.SiriOrbVisualizer
import com.example.ui.theme.SiriBlue
import com.example.ui.theme.SiriBorder
import com.example.ui.theme.SiriCanvas
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriGreen
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriOrange
import com.example.ui.theme.SiriPurple
import com.example.ui.theme.SiriSurface
import com.example.ui.theme.SiriSurfaceVariant
import com.example.ui.theme.SiriTextMuted
import com.example.ui.theme.SiriTextPrimary
import com.example.ui.theme.SiriTextSecondary
import com.example.ui.viewmodel.SiriViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiriMainScreen(
    viewModel: SiriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val languageMode by viewModel.languageMode.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val isScreenOffSimulated by viewModel.isScreenOffSimulated.collectAsState()
    val activeTimer by viewModel.activeTimer.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var manualInputText by remember { mutableStateOf("") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showServiceDialog by remember { mutableStateOf(false) }

    // Permission check for Record Audio
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // If Screen-Off Simulation is active, display the simulated screen-off view
    if (isScreenOffSimulated) {
        ScreenOffSimulationView(
            onWakeUp = { viewModel.onWakeWordDetected() },
            onExit = { viewModel.toggleScreenOffSimulation(false) }
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SiriCanvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = SiriCanvas
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation & Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Siri",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SiriSurfaceVariant,
                            border = BorderStroke(1.dp, SiriBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = SiriGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Offline AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SiriGreen
                                )
                            }
                        }
                    }
                    Text(
                        text = "iphone ni siri • સ્ક્રીન બંધ મોડ",
                        fontSize = 11.sp,
                        color = SiriTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language Switcher Button
                    Box {
                        IconButton(
                            onClick = { showLanguageMenu = true },
                            modifier = Modifier.testTag("lang_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Select Language",
                                tint = SiriCyan
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier.background(SiriSurface)
                        ) {
                            LanguageMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.displayName,
                                            color = if (mode == languageMode) SiriCyan else SiriTextPrimary,
                                            fontWeight = if (mode == languageMode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setLanguageMode(mode)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Simulated Screen Off Mode Button
                    IconButton(
                        onClick = { viewModel.toggleScreenOffSimulation(true) },
                        modifier = Modifier.testTag("screen_off_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Test Screen Off",
                            tint = if (isServiceRunning) SiriMagenta else SiriTextSecondary
                        )
                    }

                    // Settings / Service Control
                    IconButton(
                        onClick = { showServiceDialog = true },
                        modifier = Modifier.testTag("service_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Wake Settings",
                            tint = SiriTextPrimary
                        )
                    }
                }
            }

            // Foreground Service Status Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { showServiceDialog = true },
                shape = RoundedCornerShape(14.dp),
                color = SiriSurface,
                border = BorderStroke(
                    1.dp,
                    if (isServiceRunning) SiriCyan.copy(alpha = 0.5f) else SiriBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) SiriGreen else SiriTextMuted)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isServiceRunning) "Hey Siri Screen-Off Service Active" else "Screen-Off Listening is Paused",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SiriTextPrimary
                            )
                            Text(
                                text = if (isServiceRunning) "Listening for wake-word even when phone is locked" else "Tap to enable 24/7 background wake-word",
                                fontSize = 10.sp,
                                color = SiriTextMuted
                            )
                        }
                    }
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { viewModel.toggleForegroundService(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SiriCyan,
                            uncheckedThumbColor = SiriTextSecondary,
                            uncheckedTrackColor = SiriSurfaceVariant
                        ),
                        modifier = Modifier.testTag("service_toggle_switch")
                    )
                }
            }

            // Active Countdown Timer Banner (if active)
            AnimatedVisibility(
                visible = activeTimer != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                activeTimer?.let { timer ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SiriOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SiriOrange.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = SiriOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val mins = timer.remainingSeconds / 60
                                val secs = timer.remainingSeconds % 60
                                Text(
                                    text = String.format("Timer: %02d:%02d remaining", mins, secs),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SiriOrange
                                )
                            }
                            IconButton(
                                onClick = { viewModel.cancelActiveTimer() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Timer",
                                    tint = SiriOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Central Siri Orb & Interactive Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable { viewModel.toggleVoiceInteraction() }
                    .testTag("siri_orb_box"),
                contentAlignment = Alignment.Center
            ) {
                SiriOrbVisualizer(
                    status = status,
                    audioLevel = audioLevel,
                    size = 175.dp
                )
            }

            // Siri State Label
            val statusText = when (status) {
                AssistantStatus.IDLE -> "Tap orb or say \"Hey Siri\" / 'હે સિરી' બોલો"
                AssistantStatus.LISTENING -> "Listening to your voice… (Speak now)"
                AssistantStatus.THINKING -> "Processing offline command…"
                AssistantStatus.SPEAKING -> "Siri is speaking…"
                AssistantStatus.ERROR -> "Ready to listen"
            }
            Text(
                text = statusText,
                color = when (status) {
                    AssistantStatus.LISTENING -> SiriCyan
                    AssistantStatus.SPEAKING -> SiriMagenta
                    AssistantStatus.THINKING -> SiriPurple
                    else -> SiriTextSecondary
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Conversation transcript card (Last message / current response)
            val latestMessage = messages.lastOrNull()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 70.dp, max = 130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SiriSurface),
                border = BorderStroke(1.dp, SiriBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (latestMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (latestMessage.sender == MessageSender.USER) "👤 You" else "✨ Siri",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (latestMessage.sender == MessageSender.USER) SiriCyan else SiriMagenta
                            )
                            if (latestMessage.actionBadge != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SiriSurfaceVariant,
                                    border = BorderStroke(1.dp, SiriBorder)
                                ) {
                                    Text(
                                        text = latestMessage.actionBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SiriGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latestMessage.text,
                            fontSize = 13.sp,
                            color = SiriTextPrimary,
                            maxLines = 3
                        )
                    } else {
                        Text(
                            text = "Say \"Hey Siri\" or pick a quick command below.",
                            fontSize = 12.sp,
                            color = SiriTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Offline Command Suggestion Chips
            Text(
                text = "⚡ Quick Offline Commands",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SiriTextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            val suggestions = listOf(
                QuickSuggestion("Torch On", "Turn on flashlight", "🔦"),
                QuickSuggestion("ટોર્ચ ચાલુ", "ટોર્ચ ચાલુ કરો", "💡"),
                QuickSuggestion("Battery", "What is my battery level?", "🔋"),
                QuickSuggestion("Time", "What time is it?", "🕒"),
                QuickSuggestion("Camera", "Open camera", "📷"),
                QuickSuggestion("1 Min Timer", "Set timer for 1 minute", "⏱️"),
                QuickSuggestion("25 * 4", "What is 25 * 4?", "🧮"),
                QuickSuggestion("Volume +", "Increase volume", "🔊"),
                QuickSuggestion("Joke", "Tell me a joke", "😄"),
                QuickSuggestion("Who are you?", "Who are you?", "✨")
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { item ->
                    Surface(
                        modifier = Modifier.clickable {
                            viewModel.submitManualCommand(item.query)
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = SiriSurfaceVariant,
                        border = BorderStroke(1.dp, SiriBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.iconEmoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                color = SiriTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Input Bar (Voice & Text Input)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualInputText,
                    onValueChange = { manualInputText = it },
                    placeholder = {
                        Text(
                            text = "Type offline command or speak…",
                            fontSize = 12.sp,
                            color = SiriTextMuted
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SiriCyan,
                        unfocusedBorderColor = SiriBorder,
                        focusedContainerColor = SiriSurface,
                        unfocusedContainerColor = SiriSurface,
                        focusedTextColor = SiriTextPrimary,
                        unfocusedTextColor = SiriTextPrimary
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (manualInputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.submitManualCommand(manualInputText)
                                    manualInputText = ""
                                },
                                modifier = Modifier.testTag("send_command_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = SiriCyan
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Pulsing Mic Button
                val micBgBrush = if (status == AssistantStatus.LISTENING) {
                    Brush.radialGradient(listOf(SiriMagenta, SiriPurple))
                } else {
                    Brush.radialGradient(listOf(SiriCyan, SiriBlue))
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(micBgBrush)
                        .clickable { viewModel.toggleVoiceInteraction() }
                        .testTag("main_mic_action_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (status == AssistantStatus.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // Service Settings & Screen-Off Information Dialog
    if (showServiceDialog) {
        AlertDialog(
            onDismissRequest = { showServiceDialog = false },
            containerColor = SiriSurface,
            title = {
                Text(
                    text = "📱 Screen-Off Siri Setup",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "How to use Siri when phone screen is OFF:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SiriCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Turn ON the 'Background & Screen-Off Service' toggle below.\n" +
                                "2. Siri holds an Android WakeLock so the microphone keeps listening when you lock the phone.\n" +
                                "3. Saying 'Hey Siri' or 'હે સિરી' turns on the screen, plays the chime, and executes your command.\n" +
                                "4. In this web emulator, tap 'Test Screen Off Mode' (Power icon at top) to test the lock screen wake-up directly!",
                        fontSize = 12.sp,
                        color = SiriTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Always-Listening Service",
                            fontSize = 13.sp,
                            color = SiriTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { viewModel.toggleForegroundService(it) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showServiceDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SiriCyan)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
