package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.example.ui.theme.GlowEmerald
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.isAppInDarkTheme

data class NavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun HealthBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    val items = listOf(
        NavItem(
            route = "home",
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        NavItem(
            route = "step_counter",
            title = "Steps",
            selectedIcon = Icons.Filled.DirectionsRun,
            unselectedIcon = Icons.Outlined.DirectionsRun,
            testTag = "nav_steps"
        ),
        NavItem(
            route = "water_tracker",
            title = "Hydration",
            selectedIcon = Icons.Filled.WaterDrop,
            unselectedIcon = Icons.Outlined.WaterDrop,
            testTag = "nav_water"
        ),
        NavItem(
            route = "stopwatch",
            title = "Stop Watch",
            selectedIcon = Icons.Filled.Timer,
            unselectedIcon = Icons.Outlined.Timer,
            testTag = "nav_stopwatch"
        ),
        NavItem(
            route = "med_reminder",
            title = "Medicine",
            selectedIcon = Icons.Filled.Medication,
            unselectedIcon = Icons.Outlined.Medication,
            testTag = "nav_med_reminder"
        )
    )

    val navBarBackground = if (isDark) {
        Color(0xD90B1426) // ~85% translucent sapphire glass
    } else {
        Color(0xF2FFFFFF) // ~95% translucent frosted glass
    }

    val topBorderBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                GlowCyan.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.15f),
                GlowEmerald.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.8f),
                Color(0xFFCBD5E1),
                Color.White.copy(alpha = 0.8f)
            )
        )
    }

    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.2.dp,
                brush = topBorderBrush,
                shape = RoundedCornerShape(24.dp)
            )
            .then(
                if (isDark) {
                    Modifier.shadow(12.dp, shape = RoundedCornerShape(24.dp), ambientColor = GlowCyan.copy(alpha = 0.2f))
                } else {
                    Modifier.shadow(6.dp, shape = RoundedCornerShape(24.dp))
                }
            )
            .background(navBarBackground)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.testTag("health_bottom_navigation")
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = if (isSelected) {
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = if (isDark) {
                                            Brush.radialGradient(
                                                listOf(
                                                    GlowCyan.copy(alpha = 0.35f),
                                                    Color.Transparent
                                                )
                                            )
                                        } else {
                                            Brush.radialGradient(
                                                listOf(
                                                    HealthEmerald.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        }
                                    )
                            } else {
                                Modifier.size(36.dp)
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (isSelected) {
                                    if (isDark) GlowCyan else HealthEmerald
                                } else {
                                    if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) {
                                if (isDark) GlowCyan else HealthEmerald
                            } else {
                                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            }
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isDark) GlowCyan else HealthEmerald,
                        selectedTextColor = if (isDark) GlowCyan else HealthEmerald,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        unselectedTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}
