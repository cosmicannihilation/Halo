package com.example.testapplication

import android.util.Log
import android.os.Bundle
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import java.util.UUID
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val XIAO_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
val XIAO_TX_UUID      = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

object HeartRateHolder {
    var bpm by mutableStateOf(75)
}

object SensorDataHolder {
    var tempC by mutableStateOf(36.8)
}

object SpO2Holder {
    var value by mutableStateOf(98.0)
}

object MotionHolder {
    var motionLabel by mutableStateOf("Still")
    var motionValue by mutableStateOf(0.0)
}

class MainActivity : ComponentActivity() {


    var dataBuffer = ""

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter

        enableEdgeToEdge()

        setContent {
            MLTestScreen()
        }

        startScan()
    }

    private fun startScan() {

        val scanner = bluetoothAdapter.bluetoothLeScanner

        scanner.startScan(object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult) {

                val device = result.device
                val name = result.scanRecord?.deviceName

                Log.d("BLE_SCAN", "Device: $name | ${device.address}")

                // ✅ Only connect to YOUR device
                if (name != null && name.contains("Halo")) {

                    Log.d("BLE", "FOUND HALO DEVICE → CONNECTING")

                    if (bluetoothGatt != null) {
                        return
                    }

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
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

            val service = gatt.getService(XIAO_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(XIAO_TX_UUID)

            if (characteristic != null) {

                Log.d("BLE", "UART characteristic FOUND")

                // 🔥 THIS is the critical part
                gatt.setCharacteristicNotification(characteristic, true)

                val descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )

                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    Log.d("BLE", "Notifications ENABLED")
                } else {
                    Log.d("BLE", "Descriptor NOT FOUND")
                }

            } else {
                Log.d("BLE", "UART characteristic NOT FOUND")
            }
        }
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "Connected")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE", "Disconnected — reconnecting")
                gatt.close()
                bluetoothGatt = null
                runOnUiThread { startScan() }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {

            val chunk = String(characteristic.value)
            Log.d("BLE_RAW", chunk)
            dataBuffer += chunk

            while (true) {

                val lineEnd = dataBuffer.indexOf("\n")
                if (lineEnd == -1) break
                val line = dataBuffer.substring(0, lineEnd).trim()
                dataBuffer = dataBuffer.substring(lineEnd + 1)

                if (!line.startsWith("VT")) {
                    Log.d("BLE_SKIP", line)
                    continue
                }

                Log.d("BLE_LINE", line)

                val parts = line.split(",")

                var hr = 0
                var tempF = 0.0
                var spo2 = SpO2Holder.value
                var motionString = MotionHolder.motionLabel

                for (part in parts) {
                    when {
                        part.startsWith("HR:") -> {
                            hr = part.removePrefix("HR:").toIntOrNull() ?: 0
                        }
                        part.startsWith("TEMP_F:") -> {
                            tempF = part.removePrefix("TEMP_F:").toDoubleOrNull() ?: 0.0
                        }
                        part.startsWith("SPO2:") -> {
                            spo2 = part.removePrefix("SPO2:")
                                .toDoubleOrNull() ?: SpO2Holder.value
                        }
                        part.startsWith("MOTION:") -> {
                            motionString = part.removePrefix("MOTION:")
                        }
                    }
                }

                if (spo2 < 85 || spo2 > 100) {
                    spo2 = SpO2Holder.value
                }

                val rawTempC = (tempF - 32.0) * (5.0 / 9.0)
                val tempC = (rawTempC + 10.0).coerceIn(35.0, 39.0)

                val motionValue = when (motionString) {
                    "Still" -> 0.0
                    "Minor Motion" -> 5.0
                    "Active Motion" -> 10.0
                    else -> 0.0
                }

                runOnUiThread {

                    val safeHr = if (hr in 50..120) hr else HeartRateHolder.bpm

                    HeartRateHolder.bpm = safeHr
                    SensorDataHolder.tempC = tempC
                    SpO2Holder.value = spo2
                    MotionHolder.motionLabel = motionString
                    MotionHolder.motionValue = motionValue
                }
            }
        }
    }


}

@Composable
fun MLTestScreen() {


    val heartRate = HeartRateHolder.bpm
    val temp = SensorDataHolder.tempC
    val spo2 = SpO2Holder.value
    val motionValue = MotionHolder.motionValue

    val safeTemp = temp.coerceIn(35.0, 39.0)
    val safeHr = heartRate.coerceIn(50, 120)

// ✅ Compute Shock Index (simple proxy)
    val estimatedSbp = 120.0 - (safeHr - 70.0) * 0.5
    val clampedSbp = estimatedSbp.coerceIn(90.0, 140.0)
    val shockIndex = safeHr / clampedSbp

    val result = HemorrhageRiskModel.predict(
        FeatureVector(
            avgHr = safeHr.toDouble(),
            temperature = safeTemp,
            bloodOxygen = spo2,
            motionEnergy = motionValue,
            shockIndex = shockIndex
        )
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
        Text("Temperature: ${"%.2f".format(temp)} °C")
        Text("SpO2: ${spo2.toInt()}%")
        Text("Shock Index: ${"%.2f".format(shockIndex)}")

        Spacer(modifier = Modifier.height(12.dp))

        Text("Motion Value: $motionValue")

        Spacer(modifier = Modifier.height(12.dp))

        Text("Probability = ${result.probability}")
    }


}
