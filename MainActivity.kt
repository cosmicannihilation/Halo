package com.example.testapplication

import android.os.Bundle
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import java.util.UUID
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

object HeartRateHolder {
    var bpm by mutableStateOf(75)
}


class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== BLE LOCATION PERMISSION (Android 9 requirement) =====
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter

        // ===========================================================

        enableEdgeToEdge()

        setContent {
            key(System.currentTimeMillis()) {
                MLTestScreen()
            }
        }

        startScan()

    }

    private fun startScan() {

        val scanner = bluetoothAdapter.bluetoothLeScanner

        scanner.startScan(object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult) {

                val device = result.device

                android.util.Log.d(
                    "BLE_SCAN",
                    "Found device: name=${device.name}, address=${device.address}"
                )

                // ✅ ONLY connect to Polar H10
                if (device.name != null && device.name.contains("Polar H10")) {

                    bluetoothAdapter.bluetoothLeScanner.stopScan(this)

                    bluetoothGatt = device.connectGatt(
                        this@MainActivity,
                        false,
                        gattCallback
                    )
                }
            }


        })
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {

            val service = gatt.getService(
                UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
            ) ?: return

            val characteristic = service.getCharacteristic(
                UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
            ) ?: return

            // Step 1 — enable locally
            gatt.setCharacteristicNotification(characteristic, true)

            // Step 2 — ENABLE NOTIFICATION ON DEVICE (CRITICAL)
            val descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )

            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }



        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {

            val hrValue = characteristic.value[1].toInt()

            runOnUiThread {
                HeartRateHolder.bpm = hrValue
                android.util.Log.d("POLAR_HR", "Heart Rate = $hrValue")
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

@Composable
fun MLTestScreen() {

    // Live heart rate coming from BLE
    val heartRate = HeartRateHolder.bpm

    // Run ML model using live HR
    val result = HemorrhageRiskModel.predict(
        FeatureVector(heartRate.toDouble())
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Hemorrhage Risk Model", fontSize = 22.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Heart Rate: $heartRate BPM")

        Spacer(modifier = Modifier.height(12.dp))

        Text("z = ${result.z}")

        Spacer(modifier = Modifier.height(12.dp))

        Text("Probability = ${result.probability}")
    }
}
