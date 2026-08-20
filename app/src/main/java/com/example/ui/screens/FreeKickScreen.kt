package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundSynthesizer
import com.example.model.Team
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun FreeKickScreen(
    userTeam: Team,
    compTeam: Team,
    onExit: () -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var attemptsLeft by remember { mutableStateOf(5) }
    var resultBanner by remember { mutableStateOf("") }

    // Ball 2D trajectory animation state
    var isShotInProgress by remember { mutableStateOf(false) }
    var ballPos by remember { mutableStateOf(Offset(0.5f, 0.85f)) }
    var ballScale by remember { mutableStateOf(1f) }
    var keeperX by remember { mutableStateOf(0.5f) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var dragEnd by remember { mutableStateOf(Offset.Zero) }

    BackHandler { onExit() }

    fun shootFreeKick(curveDeltaX: Float, powerY: Float) {
        if (isShotInProgress || attemptsLeft <= 0) return
        isShotInProgress = true
        attemptsLeft--
        SoundSynthesizer.playKick(0.9f)

        val targetX = (0.5f + curveDeltaX * 0.003f).coerceIn(0.15f, 0.85f)
        val targetY = (0.25f - powerY * 0.0015f).coerceIn(0.18f, 0.45f)
        val keeperTarget = (0.3f + Random.nextFloat() * 0.4f)
        keeperX = keeperTarget

        // Check if scored
        val hitWall = targetY > 0.42f && targetX in 0.35f..0.65f
        val savedByKeeper = !hitWall && hypot(targetX - keeperTarget, targetY - 0.28f) < 0.12f

        resultBanner = when {
            hitWall -> {
                SoundSynthesizer.playPostHit()
                "🛡️ BLOCKED BY DEFENSE WALL!"
            }
            savedByKeeper -> {
                SoundSynthesizer.playPostHit()
                "🧤 SAVED BY GOALKEEPER!"
            }
            else -> {
                score += 100
                SoundSynthesizer.playGoalCheer()
                "🎯 TOP CORNER GOAL! (+100 PTS)"
            }
        }
    }

    LaunchedEffect(isShotInProgress) {
        if (isShotInProgress) {
            delay(1800)
            isShotInProgress = false
            ballPos = Offset(0.5f, 0.85f)
            ballScale = 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("screen_freekick")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onExit, modifier = Modifier.testTag("btn_freekick_back")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "FREE KICK CHALLENGE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "SCORE: $score",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stadium Field Box
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1B5E20))
                    .pointerInput(isShotInProgress) {
                        if (!isShotInProgress && attemptsLeft > 0) {
                            detectDragGestures(
                                onDragStart = { offset -> dragStart = offset },
                                onDrag = { change, _ -> dragEnd = change.position },
                                onDragEnd = {
                                    val dx = dragEnd.x - dragStart.x
                                    val dy = dragStart.y - dragEnd.y
                                    shootFreeKick(curveDeltaX = dx, powerY = dy)
                                }
                            )
                        }
                    }
            ) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Pitch turf gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                        )
                    )

                    // Goal Posts
                    val goalL = w * 0.15f
                    val goalR = w * 0.85f
                    val goalT = h * 0.12f
                    val goalB = h * 0.42f

                    drawRect(
                        color = Color(0x22000000),
                        topLeft = Offset(goalL, goalT),
                        size = Size(goalR - goalL, goalB - goalT)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(goalL, goalT),
                        size = Size(goalR - goalL, goalB - goalT),
                        style = Stroke(width = 5.dp.toPx())
                    )

                    // Target rings in corners
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 20.dp.toPx(),
                        center = Offset(goalL + 35.dp.toPx(), goalT + 30.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 20.dp.toPx(),
                        center = Offset(goalR - 35.dp.toPx(), goalT + 30.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Defensive Wall of 3 Players
                    val wallY = h * 0.52f
                    for (i in -1..1) {
                        val wx = w * 0.5f + i * 26.dp.toPx()
                        drawCircle(
                            color = compTeam.primaryColor,
                            radius = 16.dp.toPx(),
                            center = Offset(wx, wallY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 16.dp.toPx(),
                            center = Offset(wx, wallY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Goalkeeper
                    val gkX = w * keeperX
                    val gkY = h * 0.28f
                    drawCircle(
                        color = Color(0xFFFF9800),
                        radius = 22.dp.toPx(),
                        center = Offset(gkX, gkY)
                    )

                    // Soccer Ball
                    val bx = if (isShotInProgress) w * keeperX else w * 0.5f
                    val by = if (isShotInProgress) h * 0.25f else h * 0.85f
                    val br = (if (isShotInProgress) 14.dp.toPx() else 22.dp.toPx())

                    drawCircle(
                        color = Color.White,
                        radius = br,
                        center = Offset(bx, by)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = br * 0.35f,
                        center = Offset(bx, by)
                    )
                }

                // Instruction / Swipe gesture hint
                if (!isShotInProgress && attemptsLeft > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xCC000000)
                    ) {
                        Text(
                            text = "👆 Swipe UP & CURVE towards the top corner!",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Result banner
                if (resultBanner.isNotEmpty() && isShotInProgress) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xDD0F172A),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F))
                    ) {
                        Text(
                            text = resultBanner,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Attempts & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attempts Remaining: $attemptsLeft / 5",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                if (attemptsLeft <= 0) {
                    Button(
                        onClick = {
                            attemptsLeft = 5
                            score = 0
                            resultBanner = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TRY AGAIN")
                    }
                }
            }
        }
    }
}
