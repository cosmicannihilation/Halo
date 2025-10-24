package com.example.testapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.testapplication.ui.theme.TestApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SimulatedSensorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SimulatedSensorApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { HeartRateDatabase.getDatabase(context) }
    val dao = remember { db.heartRateDao() }

    var isStreaming by remember { mutableStateOf(false) }
    var heartRate by remember { mutableStateOf(75) }
    var recordCount by remember { mutableStateOf(0) }
    var lastStatus by remember { mutableStateOf("None") }

    val coroutineScope = rememberCoroutineScope()

    // Thresholds
    val lowBpmThreshold = 60
    val highBpmThreshold = 120
    val isOutOfRange = heartRate < lowBpmThreshold || heartRate > highBpmThreshold

    // Infinite flashing animation
    val infiniteTransition = rememberInfiniteTransition(label = "flashingBorder")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashingAlpha"
    )

    val borderColor = if (isOutOfRange) Color.Red.copy(alpha = alpha) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(width = 20.dp, color = borderColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Simulated Heart Rate Monitor", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "$heartRate bpm",
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOutOfRange) Color.Red else Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Records stored: $recordCount")
            Text("Last status: $lastStatus")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isStreaming = !isStreaming
                    if (isStreaming) {
                        coroutineScope.launch {
                            while (isStreaming) {
                                val newRate = Random.nextInt(50, 131)
                                heartRate = newRate

                                val status = when {
                                    newRate < lowBpmThreshold -> "Low"
                                    newRate > highBpmThreshold -> "High"
                                    else -> "Normal"
                                }

                                // Save to database
                                dao.insert(
                                    HeartRateReading(
                                        timestamp = System.currentTimeMillis(),
                                        heartRate = newRate,
                                        status = status
                                    )
                                )

                                recordCount = dao.getCount()
                                lastStatus = dao.getLatest()?.status ?: "None"

                                delay(1000)
                            }
                        }
                    }
                }
            ) {
                Text(if (isStreaming) "Stop Stream" else "Start Stream")
            }
        }
    }
}
