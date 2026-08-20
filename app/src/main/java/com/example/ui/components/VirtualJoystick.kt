package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Vec2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    radiusDp: Float = 60f,
    onDirectionChange: (Vec2) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size((radiusDp * 2).dp)
            .clip(CircleShape)
            .background(Color(0x55000000))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val drag = offset - center
                        val dist = hypot(drag.x, drag.y)
                        val maxR = size.width / 2f
                        val clamped = if (dist > maxR) {
                            drag * (maxR / dist)
                        } else {
                            drag
                        }
                        thumbOffset = clamped
                        onDirectionChange(Vec2(clamped.x / maxR, clamped.y / maxR))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val drag = change.position - center
                        val dist = hypot(drag.x, drag.y)
                        val maxR = size.width / 2f
                        val clamped = if (dist > maxR) {
                            drag * (maxR / dist)
                        } else {
                            drag
                        }
                        thumbOffset = clamped
                        onDirectionChange(Vec2(clamped.x / maxR, clamped.y / maxR))
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onDirectionChange(Vec2.Zero)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onDirectionChange(Vec2.Zero)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Base ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x66FFFFFF),
                radius = size.minDimension / 2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color(0x33FFFFFF),
                radius = size.minDimension / 4f
            )
        }

        // Thumb stick knob
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size((radiusDp * 0.85f).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD54F),
                            Color(0xFFF57F17),
                            Color(0xFFE65100)
                        )
                    )
                )
        )
    }
}

@Composable
fun GameActionControls(
    modifier: Modifier = Modifier,
    isChargingShoot: Boolean,
    shootCharge: Float,
    stamina: Float,
    onShootPress: () -> Unit,
    onShootRelease: () -> Unit,
    onPassClick: () -> Unit,
    onLobClick: () -> Unit,
    onSlideTackleClick: () -> Unit,
    onSprintToggle: (Boolean) -> Unit
) {
    var sprintActive by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row: Lob & Sprint
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lob Pass button
            ActionButton(
                label = "CHIP",
                subLabel = "Lob",
                icon = Icons.Default.NorthEast,
                color = Color(0xFF0288D1),
                size = 54,
                testTag = "btn_lob",
                onClick = onLobClick
            )

            // Sprint button
            FilledIconButton(
                onClick = {
                    sprintActive = !sprintActive
                    onSprintToggle(sprintActive)
                },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("btn_sprint"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (sprintActive) Color(0xFFFF9800) else Color(0x99263238),
                    contentColor = Color.White
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Sprint",
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "SPRINT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom row: Slide Tackle, Pass, Shoot
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Slide Tackle / Press
            ActionButton(
                label = "SLIDE",
                subLabel = "Tackle",
                icon = Icons.Default.SwapHoriz,
                color = Color(0xFFE53935),
                size = 56,
                testTag = "btn_slide",
                onClick = onSlideTackleClick
            )

            // Pass button
            ActionButton(
                label = "PASS",
                subLabel = "Short",
                icon = Icons.Default.SportsSoccer,
                color = Color(0xFF43A047),
                size = 58,
                testTag = "btn_pass",
                onClick = onPassClick
            )

            // Shoot Button with Power Charge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .testTag("btn_shoot")
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isChargingShoot) listOf(
                                Color(0xFFFF1744),
                                Color(0xFFFF5252),
                                Color(0xFFD50000)
                            ) else listOf(
                                Color(0xFFFF6D00),
                                Color(0xFFFFAB00),
                                Color(0xFFFF8F00)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onShootPress() },
                            onDrag = { _, _ -> },
                            onDragEnd = { onShootRelease() },
                            onDragCancel = { onShootRelease() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Power charge circular progress indicator
                if (isChargingShoot) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFFFFFF00),
                            startAngle = -90f,
                            sweepAngle = shootCharge * 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx())
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Shoot",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isChargingShoot && shootCharge > 0.85f) "SUPER!" else "SHOOT",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    size: Int,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(size.dp)
            .testTag(testTag),
        shape = CircleShape,
        color = color.copy(alpha = 0.92f),
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size((size * 0.38f).dp))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
