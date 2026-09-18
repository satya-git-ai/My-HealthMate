package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RouteCoordinate
import com.example.ui.theme.HealthBlue
import com.example.ui.theme.HealthEmerald
import com.example.ui.theme.HealthRose

@Composable
fun InteractiveRouteMap(
    routePoints: List<RouteCoordinate>,
    currentLat: Double?,
    currentLng: Double?,
    accuracyMeters: Float?,
    isTracking: Boolean,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .testTag("interactive_route_map")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
            val gridColor = if (isDark) Color(0x1FFFFFFF) else Color(0x18000000)
            val pathColor = HealthBlue
            val startColor = HealthEmerald
            val endColor = HealthRose

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4.0f)
                            panOffsetX += pan.x
                            panOffsetY += pan.y
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw map terrain grid lines
                val gridSize = 50.dp.toPx()
                var gx = (panOffsetX % gridSize)
                while (gx < w) {
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), 1f)
                    gx += gridSize
                }
                var gy = (panOffsetY % gridSize)
                while (gy < h) {
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 1f)
                    gy += gridSize
                }

                if (routePoints.isEmpty()) {
                    // Empty state: draw concentric radar circles indicating scanning/waiting
                    val center = Offset(w / 2f + panOffsetX, h / 2f + panOffsetY)
                    drawCircle(color = gridColor, radius = 60.dp.toPx(), center = center, style = Stroke(2f))
                    drawCircle(color = gridColor, radius = 120.dp.toPx(), center = center, style = Stroke(2f))
                    drawCircle(color = pathColor, radius = 8.dp.toPx(), center = center)
                    return@Canvas
                }

                // Compute bounding box of coordinates
                val lats = routePoints.map { it.latitude }
                val lngs = routePoints.map { it.longitude }
                val minLat = lats.minOrNull() ?: 0.0
                val maxLat = lats.maxOrNull() ?: 0.0
                val minLng = lngs.minOrNull() ?: 0.0
                val maxLng = lngs.maxOrNull() ?: 0.0

                val latSpan = (maxLat - minLat).coerceAtLeast(0.0008)
                val lngSpan = (maxLng - minLng).coerceAtLeast(0.0008)

                val padding = 60.dp.toPx()
                val drawW = w - padding * 2
                val drawH = h - padding * 2

                fun toScreenOffset(lat: Double, lng: Double): Offset {
                    val nx = ((lng - minLng) / lngSpan).toFloat()
                    val ny = (1f - ((lat - minLat) / latSpan).toFloat()) // Invert Y because screen Y goes down

                    val basePoint = Offset(
                        padding + nx * drawW,
                        padding + ny * drawH
                    )

                    // Apply zoom & pan around canvas center
                    val cx = w / 2f
                    val cy = h / 2f
                    val zx = (basePoint.x - cx) * zoomScale + cx + panOffsetX
                    val zy = (basePoint.y - cy) * zoomScale + cy + panOffsetY
                    return Offset(zx, zy)
                }

                // Draw Route Polyline
                if (routePoints.size >= 2) {
                    val path = Path()
                    val firstPoint = toScreenOffset(routePoints.first().latitude, routePoints.first().longitude)
                    path.moveTo(firstPoint.x, firstPoint.y)

                    for (i in 1 until routePoints.size) {
                        val pt = toScreenOffset(routePoints[i].latitude, routePoints[i].longitude)
                        path.lineTo(pt.x, pt.y)
                    }

                    // Draw outer subtle glow
                    drawPath(
                        path = path,
                        color = pathColor.copy(alpha = 0.25f),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw main track line
                    drawPath(
                        path = path,
                        color = pathColor,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw Start Point Marker (Green Circle)
                val startScreen = toScreenOffset(routePoints.first().latitude, routePoints.first().longitude)
                drawCircle(color = startColor, radius = 9.dp.toPx(), center = startScreen)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = startScreen)

                // Draw End / Current Point Marker (Red or Active Pulsing Blue)
                val lastScreen = toScreenOffset(routePoints.last().latitude, routePoints.last().longitude)
                val markerColor = if (isTracking) pathColor else endColor
                drawCircle(color = markerColor.copy(alpha = 0.3f), radius = 16.dp.toPx(), center = lastScreen)
                drawCircle(color = markerColor, radius = 9.dp.toPx(), center = lastScreen)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = lastScreen)
            }

            // Top Status Overlay (Coordinates / Accuracy)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "GPS",
                        tint = if (isTracking) HealthEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentLat != null && currentLng != null) {
                            "%.4f, %.4f (%dm)".format(currentLat, currentLng, (accuracyMeters ?: 0f).toInt())
                        } else if (isTracking) {
                            "Acquiring GPS fix..."
                        } else {
                            "GPS Idle"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Controls on the Right: Zoom in, Zoom out, Recenter
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(4f) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.5f) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    IconButton(
                        onClick = {
                            panOffsetX = 0f
                            panOffsetY = 0f
                            zoomScale = 1.0f
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Recenter", tint = HealthBlue)
                    }
                }
            }
        }
    }
}
