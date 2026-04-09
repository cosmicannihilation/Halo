package com.example.testapplication

import kotlin.math.exp
import kotlin.math.max

data class FeatureVector(
    val avgHr: Double,
    val temperature: Double,
    val bloodOxygen: Double,
    val motionEnergy: Double,
    val pressure: Double   // ✅ NEW
)

data class RiskResult(
    val hr: Double,
    val z: Double,
    val probability: Double
)

object HemorrhageRiskModel {

    private const val wHr = 0.13268602081980885
    private const val wTemp = 2.4950110286339506
    private const val wO2 = 0.1256589842699974
    private const val wMotion = -0.002864827478843167
    private const val wPressure = 0.2754706460094852   // ✅ NEW
    private const val bias = -119.82769906472619

    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x))
    }

    private fun relu(x: Double): Double {
        return max(0.0, x)
    }

    fun predict(features: FeatureVector): RiskResult {

        val hr = features.avgHr.coerceIn(40.0, 150.0)
        val temp = features.temperature.coerceIn(34.0, 40.0)
        val spo2 = features.bloodOxygen.coerceIn(85.0, 100.0)
        val motion = features.motionEnergy.coerceIn(0.0, 10.0)
        val pressure = features.pressure.coerceIn(0.0, 1000.0) // ✅ NEW

        val hr_n = (hr - 60.0) / 60.0
        val temp_n = (temp - 36.0) / 3.0
        val spo2_n = (spo2 - 90.0) / 10.0
        val motion_n = motion / 10.0
        val pressure_n = pressure / 1000.0   // ✅ NEW

        val h1 = relu(
            (wHr * hr_n) +
                    (wTemp * temp_n) +
                    (wO2 * spo2_n) +
                    (wMotion * motion_n) +
                    (wPressure * pressure_n) +   // ✅ NEW
                    0.1
        )

        val h2 = relu(
            (0.5 * wHr * hr_n) -
                    (0.7 * wTemp * temp_n) +
                    (1.2 * wO2 * spo2_n) +
                    (0.3 * wMotion * motion_n) +
                    (0.5 * wPressure * pressure_n) -   // ✅ NEW
                    0.05
        )

        val h3 = relu(
            (-0.3 * wHr * hr_n) +
                    (0.9 * wTemp * temp_n) -
                    (0.8 * wO2 * spo2_n) +
                    (0.2 * wMotion * motion_n) +
                    (0.3 * wPressure * pressure_n) +   // ✅ NEW
                    0.02
        )

        val z =
            (1.2 * h1) +
                    (-1.0 * h2) +
                    (0.8 * h3) +
                    (bias * 0.01)

        val probability = sigmoid(z)

        return RiskResult(
            hr = hr,
            z = z,
            probability = probability
        )
    }

    fun computeRisk(features: FeatureVector): Double {
        return predict(features).probability
    }
}
