package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SiriBlue
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriPurple
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenOffSimulationView(
    onWakeUp: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFmt.format(now)
            currentDate = dateFmt.format(now)
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("screen_off_simulation_view")
    ) {
        // Exit button at top right
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .testTag("exit_screen_off_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Screen Off Mode",
                tint = Color.White.copy(alpha = 0.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Ambient Lock Screen Clock (OLED Low-Power aesthetic)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentDate,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTime.ifEmpty { "12:00" },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141824).copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3A59))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SiriCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phone Locked • Screen Sleeping",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Central Wake Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎙️ Background Listening Active",
                    color = SiriCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Say \"Hey Siri\" or \"હે સિરી\" aloud,\nor tap below to trigger Siri:",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Wake Trigger Siri Orb Button
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SiriMagenta.copy(alpha = 0.8f),
                                    SiriPurple.copy(alpha = 0.7f),
                                    SiriCyan.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(2.dp, SiriCyan.copy(alpha = 0.8f), CircleShape)
                        .clickable { onWakeUp() }
                        .testTag("simulate_wake_word_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Trigger Wake Word",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "Hey Siri",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Bottom explanation
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "This mode simulates your phone with its screen off.\nSiri's wake engine responds instantly to commands.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2538)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("wake_screen_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn Screen Back On", fontSize = 13.sp)
                }
            }
        }
    }
}
