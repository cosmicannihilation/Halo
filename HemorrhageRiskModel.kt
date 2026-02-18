package com.example.testapplication

import kotlin.math.exp

/**
 * Hemorrhage Risk Model
 *
 * IMPORTANT:
 * This model was trained OFFLINE using external data.
 * The learned parameters (weight and bias) are embedded
 * directly into this file for on-device inference only.
 * No training occurs on the Android device. The application performs real-time inference using
 * live heart-rate data streamed from a Polar H10 sensor.
 */



/**
 * Feature container used by the ML model.
 * Currently only heart rate is required.
 */
data class FeatureVector(
    val avgHr: Double
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
 * z = w*x + b
 * risk = sigmoid(z)
 */
object HemorrhageRiskModel {

    // ===== Trained Parameters (HR-only model) =====
    private const val weightHr = 0.025592365059655283
    private const val bias = -8.616429237372008

    /**
     * Main prediction function used for UI + debugging display.
     */
    fun predict(features: FeatureVector): RiskResult {

        val z = (weightHr * features.avgHr) + bias
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
