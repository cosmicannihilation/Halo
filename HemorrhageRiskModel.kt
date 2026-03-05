package com.example.testapplication

import kotlin.math.exp

/**
 * Hemorrhage Risk Model
 *
 * IMPORTANT:
 * This model was trained OFFLINE using external data.
 * The learned parameters (weights and bias) are embedded
 * directly into this file for on-device inference only.
 * No training occurs on the Android device.
 */

/**
 * Feature container used by the ML model.
 */
data class FeatureVector(
    val avgHr: Double,
    val temperature: Double,
    val bloodOxygen: Double,
    val motionEnergy: Double
)

/**
 * Result container so we can display
 * intermediate ML calculations live.
 */
data class RiskResult(
    val hr: Double,
    val z: Double,
    val probability: Double
)

/**
 * Hemorrhage Risk Model
 *
 * Logistic Regression
 */
object HemorrhageRiskModel {

    // ===== Trained Parameters =====
    private const val weightHr = 0.13203401111307586
    private const val weightTemp = 2.4351717228913006
    private const val weightOxygen = 0.12205877818071573
    private const val weightMotion = -0.016754381194854184
    private const val bias = -116.83369778573851

    /**
     * Main prediction function
     */
    fun predict(features: FeatureVector): RiskResult {

        val z =
            (weightHr * features.avgHr) +
                    (weightTemp * features.temperature) +
                    (weightOxygen * features.bloodOxygen) +
                    (weightMotion * features.motionEnergy) +
                    bias

        val probability = sigmoid(z)

        return RiskResult(
            hr = features.avgHr,
            z = z,
            probability = probability
        )
    }

    fun computeRisk(features: FeatureVector): Double {
        return predict(features).probability
    }

    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x))
    }
}
