package com.example.testapplication

import kotlin.math.exp

data class FeatureVector(
    val avgHr: Double,
    val temperature: Double,
    val bloodOxygen: Double,
    val motionEnergy: Double,
    val shockIndex: Double   // ✅ NEW
)

data class RiskResult(
    val hr: Double,
    val z: Double,
    val probability: Double
)

object HemorrhageRiskModel {


    // ✅ NEW weights from retrained model
    private const val wHr = 2.9991944183942953
    private const val wTemp = 0.20023356674150594
    private const val wO2 = 0.36841213040037774
    private const val wMotion = 0.4297996702236205
    private const val wSI = 4.878054193886159
    private const val bias = -4.242268434406739

    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x))
    }

    fun predict(features: FeatureVector): RiskResult {

        val hr = features.avgHr.coerceIn(40.0, 150.0)
        val temp = features.temperature.coerceIn(34.0, 40.0)
        val spo2 = features.bloodOxygen.coerceIn(85.0, 100.0)
        val motion = features.motionEnergy.coerceIn(0.0, 10.0)
        val si = features.shockIndex.coerceIn(0.3, 2.0)

        // ✅ MUST match Python normalization EXACTLY
        val hr_n = (hr - 60.0) / 60.0
        val temp_n = (temp - 36.5) / 2.5
        val spo2_n = (spo2 - 95.0) / 5.0
        val motion_n = motion / 10.0
        val si_n = (si - 0.7) / 0.5

        val z =
            (wHr * hr_n) +
                    (wTemp * temp_n) +
                    (wO2 * spo2_n) +
                    (wMotion * motion_n) +
                    (wSI * si_n) +
                    bias

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
