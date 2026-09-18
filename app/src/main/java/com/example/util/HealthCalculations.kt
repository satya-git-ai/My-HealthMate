package com.example.util

import kotlin.math.pow
import kotlin.math.roundToInt

data class HealthRecommendation(
    val bmi: Float,
    val bmiCategory: String,
    val bmiColorHex: Long,
    val recommendedDailySteps: Int,
    val recommendedDailyMinutes: Int,
    val recommendedDailyWaterMl: Int,
    val estimatedDailyBurnKcal: Int,
    val summaryAdvice: String
)

data class WeightLossCalcResult(
    val weightToLoseKg: Float,
    val totalCalorieDeficitKcal: Int,
    val weeklyCalorieDeficitKcal: Int,
    val dailyCalorieDeficitKcal: Int,
    val dailyStepsNeeded: Int,
    val dailyDistanceKm: Float,
    val dailyWalkingMinutes: Int,
    val weeklyDistanceKm: Float,
    val weeklyStepsNeeded: Int,
    val weeklyWeightLossRateKg: Float,
    val isSafeRate: Boolean,
    val safetyStatusText: String,
    val safetyColorHex: Long
)

enum class WalkingPace(
    val label: String,
    val speedKmH: Float,
    val metValue: Float,
    val description: String
) {
    LEISURELY("Leisurely (3.5 km/h)", 3.5f, 2.8f, "Casual stroll, window shopping"),
    MODERATE("Moderate (4.8 km/h)", 4.8f, 3.5f, "Standard walking pace, regular commute"),
    BRISK("Brisk (6.0 km/h)", 6.0f, 4.3f, "Fast-paced fitness walking with arm motion"),
    POWER("Power Walk (7.2 km/h)", 7.2f, 5.0f, "High-intensity vigorous athletic walking")
}

object HealthCalculations {

    /**
     * Calculates BMI = weight (kg) / (height (m) ^ 2)
     */
    fun calculateBmi(weightKg: Float, heightCm: Float): Float {
        if (heightCm <= 0 || weightKg <= 0) return 22.0f
        val heightM = heightCm / 100f
        return (weightKg / heightM.pow(2) * 10f).roundToInt() / 10f
    }

    /**
     * Categorizes BMI with color and medical description
     */
    fun getBmiCategory(bmi: Float): Pair<String, Long> {
        return when {
            bmi < 18.5f -> "Underweight" to 0xFF38BDF8
            bmi in 18.5f..24.9f -> "Healthy Weight" to 0xFF10B981
            bmi in 25.0f..29.9f -> "Overweight" to 0xFFF59E0B
            else -> "Obesity Range" to 0xFFEF4444
        }
    }

    /**
     * Calculates personalized step recommendations based on Weight, Age, and Primary Goal
     * e.g., For a person weighing 55 kg, walking 30 to 45 minutes daily (about 7,000 to 10,000 steps)
     * is ideal for general health and weight management.
     */
    fun calculatePersonalizedRecommendation(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String = "Male",
        primaryGoal: String = "General Fitness"
    ): HealthRecommendation {
        val safeWeight = if (weightKg > 20f) weightKg else 70f
        val safeHeight = if (heightCm > 50f) heightCm else 175f
        val safeAge = if (age in 10..110) age else 28

        val bmi = calculateBmi(safeWeight, safeHeight)
        val (category, colorHex) = getBmiCategory(bmi)

        // Stride length in meters: approx 41.4% of height
        val strideMeters = (safeHeight * 0.414f) / 100f

        // Base step calculation from weight and goal
        val baseSteps = when {
            safeWeight <= 55f -> 8000
            safeWeight in 55.1f..75f -> 9500
            safeWeight in 75.1f..90f -> 10500
            else -> 11500
        }

        val goalModifier = when (primaryGoal) {
            "Weight Loss" -> 1.25f
            "Cardio & Heart" -> 1.15f
            "Habit Building" -> 0.85f
            else -> 1.0f // General Fitness
        }

        val ageModifier = when {
            safeAge < 30 -> 1.05f
            safeAge in 30..49 -> 1.0f
            safeAge in 50..64 -> 0.95f
            else -> 0.85f
        }

        val recommendedSteps = ((baseSteps * goalModifier * ageModifier) / 500).roundToInt() * 500
        val boundedSteps = recommendedSteps.coerceIn(5000, 20000)

        // Estimated walking minutes (at ~100 steps per min)
        val walkingMinutes = (boundedSteps / 100).coerceIn(30, 90)

        // Recommended Water: ~35 ml per kg of body weight
        val rawWaterMl = (safeWeight * 35f).roundToInt()
        val recommendedWater = ((rawWaterMl + 100) / 250) * 250 // rounded to nearest 250ml

        // Estimated daily calorie burn from walking
        val caloriesPerStep = 0.04f * (safeWeight / 70f)
        val estimatedBurn = (boundedSteps * caloriesPerStep).roundToInt()

        val summary = "For a person weighing ${safeWeight.roundToInt()} kg and age $safeAge, walking $walkingMinutes minutes daily (about ${String.format("%,d", boundedSteps)} steps) is ideal for ${primaryGoal.lowercase()} and overall health management."

        return HealthRecommendation(
            bmi = bmi,
            bmiCategory = category,
            bmiColorHex = colorHex,
            recommendedDailySteps = boundedSteps,
            recommendedDailyMinutes = walkingMinutes,
            recommendedDailyWaterMl = recommendedWater.coerceIn(1500, 5000),
            estimatedDailyBurnKcal = estimatedBurn,
            summaryAdvice = summary
        )
    }

    /**
     * GetSteps.app Weight Loss Walking Calculator Engine
     * 7,700 kcal deficit = 1 kg body fat loss
     */
    fun calculateWeightLossWalking(
        currentWeightKg: Float,
        targetWeightKg: Float,
        timeframeWeeks: Int,
        walkingDaysPerWeek: Int,
        walkingPace: WalkingPace,
        userHeightCm: Float = 175f
    ): WeightLossCalcResult {
        val safeCurrent = currentWeightKg.coerceIn(30f, 250f)
        val safeTarget = targetWeightKg.coerceIn(25f, safeCurrent)
        val safeWeeks = timeframeWeeks.coerceIn(1, 52)
        val safeDays = walkingDaysPerWeek.coerceIn(1, 7)
        val safeHeight = userHeightCm.coerceIn(100f, 230f)

        val weightToLose = (safeCurrent - safeTarget).coerceAtLeast(0.1f)
        val totalCalorieDeficit = (weightToLose * 7700f).roundToInt()

        val weeklyCalorieDeficit = (totalCalorieDeficit / safeWeeks.toFloat()).roundToInt()
        val dailyCalorieDeficit = (weeklyCalorieDeficit / safeDays.toFloat()).roundToInt()

        val weeklyLossRate = weightToLose / safeWeeks.toFloat()

        // Calorie burn rate calculations based on MET and Pace
        // Calories per hour = MET * weight(kg)
        val caloriesPerHour = walkingPace.metValue * safeCurrent
        val caloriesPerKm = caloriesPerHour / walkingPace.speedKmH

        // Stride Length in meters
        val strideMeters = (safeHeight * 0.414f) / 100f
        val stepsPerKm = (1000f / strideMeters).roundToInt()
        val caloriesPerStep = caloriesPerKm / stepsPerKm.toFloat()

        val dailyStepsNeeded = (dailyCalorieDeficit / caloriesPerStep).roundToInt().coerceAtLeast(1000)
        val dailyDistanceKm = (dailyStepsNeeded * strideMeters / 1000f * 10f).roundToInt() / 10f
        val dailyMinutes = (dailyDistanceKm / walkingPace.speedKmH * 60f).roundToInt().coerceAtLeast(10)

        val weeklyDistanceKm = (dailyDistanceKm * safeDays * 10f).roundToInt() / 10f
        val weeklyStepsNeeded = dailyStepsNeeded * safeDays

        // Safe rate evaluation (0.5 kg to 1.0 kg per week is clinically recommended)
        val isSafe = weeklyLossRate <= 1.05f
        val (statusText, statusColor) = when {
            weeklyLossRate <= 0.55f -> "Optimal & Sustainable Pace (≤ 0.5 kg/week)" to 0xFF10B981
            weeklyLossRate <= 1.05f -> "Moderately Aggressive Pace (0.5 - 1.0 kg/week)" to 0xFFF59E0B
            else -> "Aggressive! Consider extending timeframe for healthier pace (> 1.0 kg/week)" to 0xFFEF4444
        }

        return WeightLossCalcResult(
            weightToLoseKg = (weightToLose * 10f).roundToInt() / 10f,
            totalCalorieDeficitKcal = totalCalorieDeficit,
            weeklyCalorieDeficitKcal = weeklyCalorieDeficit,
            dailyCalorieDeficitKcal = dailyCalorieDeficit,
            dailyStepsNeeded = dailyStepsNeeded,
            dailyDistanceKm = dailyDistanceKm,
            dailyWalkingMinutes = dailyMinutes,
            weeklyDistanceKm = weeklyDistanceKm,
            weeklyStepsNeeded = weeklyStepsNeeded,
            weeklyWeightLossRateKg = (weeklyLossRate * 100f).roundToInt() / 100f,
            isSafeRate = isSafe,
            safetyStatusText = statusText,
            safetyColorHex = statusColor
        )
    }
}
