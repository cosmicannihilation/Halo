
package com.example.testapplication

import kotlin.math.exp

/**
 * Hemorrhage Risk Model v2.0 NOW INCLUDES HEARTRATE + TEMPERATURE
 *
 * IMPORTANT:
 * This model was trained OFFLINE using external data.
 * The learned parameters (weights and bias) are embedded
 * directly into this file for on-device inference only.
 * No training occurs on the Android device.
 * The application performs real-time inference using
 * live physiological data streamed from sensors.
 */


/**
 * Feature container used by the ML model.
 * Now supports heart rate + temperature.
 */
data class FeatureVector(
    val avgHr: Double,
    val temperature: Double
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
 * ON-DEVICE INFERENCE ONLY
 * Logistic Regression:
 *
 * z = w1*x1 + w2*x2 + b
 * risk = sigmoid(z)
 */
object HemorrhageRiskModel {

    // ===== Trained Parameters (HR + Temperature model) =====
    private const val weightHr = 0.13071517041228678
    private const val weightTemp = 2.4987513048592267
    private const val bias = -107.60939401683565

    /**
     * Main prediction function used for UI + debugging display.
     */
    fun predict(features: FeatureVector): RiskResult {

        val z =
            (weightHr * features.avgHr) +
                    (weightTemp * features.temperature) +
                    bias

        val probability = sigmoid(z)

        return RiskResult(
            hr = features.avgHr,
            z = z,
            probability = probability
        )
    }

    /**
     * Backwards-compatible function.
     * If other parts of your app call computeRisk(),
     * nothing breaks.
     */
    fun computeRisk(features: FeatureVector): Double {
        return predict(features).probability
    }

    /**
     * Sigmoid activation function.
     */
    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x))
    }
}
