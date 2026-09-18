package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserSettings
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowEmerald
import com.example.ui.theme.GlowOrange
import com.example.ui.theme.GlowPurple
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthCyan
import com.example.ui.theme.HealthEmerald
import com.example.util.HealthCalculations
import kotlin.math.roundToInt

data class OnboardingFeaturePage(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

@Composable
fun OnboardingScreen(
    userSettings: UserSettings,
    onSaveProfileAndFinish: (weightKg: Float, heightCm: Float, age: Int, gender: String, goal: String, targetWeightKg: Float) -> Unit,
    initialStepIndex: Int = 0,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var stepIndex by remember { mutableIntStateOf(initialStepIndex) } // 0: Feature Intro, 1: Profile Customizer Prompt

    // Profile state
    var weightKg by remember { mutableFloatStateOf(userSettings.userWeightKg.coerceIn(35f, 160f)) }
    var heightCm by remember { mutableFloatStateOf(userSettings.userHeightCm.coerceIn(120f, 220f)) }
    var age by remember { mutableIntStateOf(userSettings.userAge.coerceIn(12, 99)) }
    var gender by remember { mutableStateOf(userSettings.userGender) }
    var primaryGoal by remember { mutableStateOf(userSettings.userPrimaryGoal) }
    var targetWeightKg by remember { mutableFloatStateOf(userSettings.targetWeightKg.coerceIn(30f, 150f)) }

    // Live dynamic recommendation based on biometrics
    val recommendation by remember(weightKg, heightCm, age, gender, primaryGoal) {
        derivedStateOf {
            HealthCalculations.calculatePersonalizedRecommendation(
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                gender = gender,
                primaryGoal = primaryGoal
            )
        }
    }

    val featurePages = listOf(
        OnboardingFeaturePage(
            title = "Precision Activity",
            subtitle = "Automatic Step Tracking",
            description = "Track your daily steps, distance, and active calories with low-battery hardware sensors.",
            icon = Icons.Default.DirectionsRun,
            gradientColors = listOf(HealthEmerald, HealthCyan)
        ),
        OnboardingFeaturePage(
            title = "Smart Hydration & Sleep",
            subtitle = "Custom Habit Cycles",
            description = "Set intelligent hydration reminders, sleep alarms, and track daily water milestones effortlessly.",
            icon = Icons.Default.WaterDrop,
            gradientColors = listOf(HealthBlue, HealthCyan)
        )
    )

    var introPageIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GlowCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GlowCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (stepIndex == 0) "Welcome to HealthMate" else "Personalize Your Plan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (stepIndex == 0) {
            // STEP 0: Feature overview slider
            Spacer(modifier = Modifier.weight(0.3f))

            AnimatedContent(
                targetState = introPageIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_animation"
            ) { pageIdx ->
                val page = featurePages[pageIdx]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .background(
                                brush = Brush.linearGradient(page.gradientColors),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = Color.White,
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = page.subtitle.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = GlowEmerald
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.7f))

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                featurePages.forEachIndexed { index, _ ->
                    val isSelected = index == introPageIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 24.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) GlowCyan else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (introPageIndex < featurePages.size - 1) {
                        introPageIndex++
                    } else {
                        stepIndex = 1
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (introPageIndex == featurePages.size - 1) "Customize my profile" else "Next",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        } else {
            // STEP 1: INTERACTIVE PROFILE PROMPT (Height, Weight, Age, Gender, Goal)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tell us about yourself to tailor your walking & hydration targets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Live Recommendation Insight Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0284C7).copy(alpha = 0.15f),
                                    Color(0xFF0D9488).copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, GlowCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = GlowCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PERSONALIZED HEALTH INSIGHT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = GlowCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Scientific dynamic output
                        Text(
                            text = recommendation.summaryAdvice,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Steps Pill
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlowCyan.copy(alpha = 0.12f))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Ideal Step Goal",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%,d", recommendation.recommendedDailySteps)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GlowCyan
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Hydration Pill
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlowEmerald.copy(alpha = 0.12f))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Daily Water",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${recommendation.recommendedDailyWaterMl} ml",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GlowEmerald
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // BMI Pill
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(recommendation.bmiColorHex).copy(alpha = 0.12f))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "BMI (${recommendation.bmiCategory.take(6)})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${recommendation.bmi}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(recommendation.bmiColorHex)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Weight Selector (kg)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = GlowCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Body Weight",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "${weightKg.roundToInt()} kg (${(weightKg * 2.20462f).roundToInt()} lbs)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = GlowCyan
                            )
                        }

                        Slider(
                            value = weightKg,
                            onValueChange = { weightKg = (it * 2).roundToInt() / 2f },
                            valueRange = 35f..150f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlowCyan,
                                activeTrackColor = GlowCyan
                            ),
                            modifier = Modifier.testTag("onboarding_weight_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Height Selector (cm)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Height, contentDescription = null, tint = GlowEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Height",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            val feet = (heightCm / 30.48f).toInt()
                            val inches = ((heightCm / 2.54f) % 12).roundToInt()
                            Text(
                                text = "${heightCm.roundToInt()} cm (${feet}'${inches}\")",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = GlowEmerald
                            )
                        }

                        Slider(
                            value = heightCm,
                            onValueChange = { heightCm = it.roundToInt().toFloat() },
                            valueRange = 120f..210f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlowEmerald,
                                activeTrackColor = GlowEmerald
                            ),
                            modifier = Modifier.testTag("onboarding_height_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Age Selector
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = GlowOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Age",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "$age years old",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = GlowOrange
                            )
                        }

                        Slider(
                            value = age.toFloat(),
                            onValueChange = { age = it.roundToInt() },
                            valueRange = 14f..85f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlowOrange,
                                activeTrackColor = GlowOrange
                            ),
                            modifier = Modifier.testTag("onboarding_age_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Gender & Primary Goal Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Gender
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gender",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Female", "Male").forEach { item ->
                                val isSelected = gender == item
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GlowCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) GlowCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { gender = item }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                        color = if (isSelected) GlowCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Goal
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "Primary Goal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("General Fitness", "Weight Loss").forEach { item ->
                                val isSelected = primaryGoal.startsWith(item.take(7))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GlowEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) GlowEmerald else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { primaryGoal = item }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item == "General Fitness") "Fitness" else "Weight Loss",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                        color = if (isSelected) GlowEmerald else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Complete Button
                Button(
                    onClick = {
                        onSaveProfileAndFinish(
                            weightKg,
                            heightCm,
                            age,
                            gender,
                            primaryGoal,
                            targetWeightKg
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_save_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
