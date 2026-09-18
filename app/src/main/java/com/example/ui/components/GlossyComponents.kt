package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSpecularLight
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.isAppInDarkTheme

/**
 * Modern Transparent Glossy Glass Card with specular highlight border and optional neon glow halo.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    glowColor: Color = GlowCyan.copy(alpha = 0.22f),
    glassAlpha: Float = 0.75f,
    borderWidth: Dp = 1.2.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isAppInDarkTheme()

    val surfaceColor = if (isDark) {
        Color(0xFF0F1A2E).copy(alpha = glassAlpha)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.88f)
    }

    val specularBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.45f),
                glowColor.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.08f),
                glowColor.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFCBD5E1).copy(alpha = 0.8f),
                glowColor.copy(alpha = 0.3f),
                Color(0xFFE2E8F0)
            )
        )
    }

    val cardModifier = modifier
        .then(
            if (isDark) {
                Modifier.shadow(
                    elevation = 8.dp,
                    shape = shape,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
            } else {
                Modifier.shadow(
                    elevation = 4.dp,
                    shape = shape,
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000)
                )
            }
        )
        .clip(shape)
        .background(
            brush = if (isDark) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF162544).copy(alpha = glassAlpha),
                        Color(0xFF0D172A).copy(alpha = glassAlpha + 0.1f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color(0xFFF8FAFC).copy(alpha = 0.85f)
                    )
                )
            }
        )
        .border(
            width = borderWidth,
            brush = specularBrush,
            shape = shape
        )
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Box(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Glowing Pill / Container for badges, metrics, and highlights
 */
@Composable
fun GlowPill(
    modifier: Modifier = Modifier,
    glowColor: Color = GlowCyan,
    backgroundColor: Color = glowColor.copy(alpha = 0.15f),
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        glowColor.copy(alpha = 0.6f),
                        glowColor.copy(alpha = 0.2f),
                        glowColor.copy(alpha = 0.5f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
