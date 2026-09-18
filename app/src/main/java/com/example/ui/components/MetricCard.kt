package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color? = null,
    testTag: String = "metric_card",
    onClick: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()

    val specularBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.35f),
                iconTint.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.05f),
                iconTint.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFCBD5E1).copy(alpha = 0.8f),
                iconTint.copy(alpha = 0.25f),
                Color(0xFFE2E8F0)
            )
        )
    }

    val cardBackground = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF132038).copy(alpha = 0.85f),
                Color(0xFF0C162A).copy(alpha = 0.92f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.92f),
                Color(0xFFF8FAFC).copy(alpha = 0.88f)
            )
        )
    }

    val glowShadowColor = if (isDark) iconTint.copy(alpha = 0.25f) else Color(0x1A000000)

    Box(
        modifier = modifier
            .testTag(testTag)
            .shadow(
                elevation = if (isDark) 6.dp else 3.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = glowShadowColor,
                spotColor = glowShadowColor
            )
            .clip(RoundedCornerShape(22.dp))
            .background(cardBackground)
            .border(
                width = 1.2.dp,
                brush = specularBrush,
                shape = RoundedCornerShape(22.dp)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(iconBgColor)
                            .border(
                                width = 1.dp,
                                brush = Brush.radialGradient(
                                    listOf(iconTint.copy(alpha = 0.6f), iconTint.copy(alpha = 0.1f))
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.8f) else Color(0xFF64748B).copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (badgeText != null) {
                    val color = badgeColor ?: iconTint
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.15f))
                            .border(
                                width = 0.8.dp,
                                color = color.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = color,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}
