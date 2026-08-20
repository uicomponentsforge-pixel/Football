package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundSynthesizer
import com.example.data.GameRecordsRepository
import com.example.model.AiDifficulty
import com.example.model.Team
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

enum class GoalZone(val label: String, val xFrac: Float, val yFrac: Float) {
    TOP_LEFT("Top Left", 0.2f, 0.25f),
    TOP_RIGHT("Top Right", 0.8f, 0.25f),
    CENTER("Center Rocket", 0.5f, 0.5f),
    BOTTOM_LEFT("Bottom Left", 0.2f, 0.75f),
    BOTTOM_RIGHT("Bottom Right", 0.8f, 0.75f)
}

enum class ShootoutTurn {
    USER_SHOOTING,
    COMP_SHOOTING,
    RESULT_ANIMATION
}

@Composable
fun PenaltyShootoutScreen(
    userTeam: Team,
    compTeam: Team,
    difficulty: AiDifficulty,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val recordsRepo = remember { GameRecordsRepository(context) }

    val userResults = remember { mutableStateListOf<Boolean>() }
    val compResults = remember { mutableStateListOf<Boolean>() }

    var currentRound by remember { mutableStateOf(1) }
    var currentTurn by remember { mutableStateOf(ShootoutTurn.USER_SHOOTING) }
    var actionResultText by remember { mutableStateOf("") }
    var isGameOver by remember { mutableStateOf(false) }

    // Ball & Keeper animation positions
    var shotTargetZone by remember { mutableStateOf<GoalZone?>(null) }
    var keeperDiveZone by remember { mutableStateOf<GoalZone?>(null) }
    var isAnimatingShot by remember { mutableStateOf(false) }

    BackHandler { onExit() }

    fun resolveShot(shot: GoalZone, keeper: GoalZone, isUserShooting: Boolean) {
        shotTargetZone = shot
        keeperDiveZone = keeper
        isAnimatingShot = true

        SoundSynthesizer.playKick(0.9f)

        val isGoal = (shot != keeper)

        if (isGoal) {
            actionResultText = if (isUserShooting) "🎉 GOAL SCORED!" else "⚽ COMPUTER SCORES"
            SoundSynthesizer.playGoalCheer()
        } else {
            actionResultText = if (isUserShooting) "🧤 SAVED BY KEEPER!" else "🧤 INCREDIBLE SAVE!"
            SoundSynthesizer.playPostHit()
        }

        if (isUserShooting) {
            userResults.add(isGoal)
        } else {
            compResults.add(isGoal)
        }
    }

    LaunchedEffect(isAnimatingShot) {
        if (isAnimatingShot) {
            delay(1600)
            isAnimatingShot = false
            shotTargetZone = null
            keeperDiveZone = null

            // Check match progression
            if (currentTurn == ShootoutTurn.USER_SHOOTING) {
                currentTurn = ShootoutTurn.COMP_SHOOTING
                SoundSynthesizer.playWhistle()
            } else {
                // Round finished
                val userGoals = userResults.count { it }
                val compGoals = compResults.count { it }

                if (currentRound >= 5) {
                    if (userGoals != compGoals) {
                        isGameOver = true
                        if (userGoals > compGoals) recordsRepo.recordPenaltyWin()
                    } else {
                        // Sudden death
                        currentRound++
                        currentTurn = ShootoutTurn.USER_SHOOTING
                        SoundSynthesizer.playWhistle()
                    }
                } else {
                    currentRound++
                    currentTurn = ShootoutTurn.USER_SHOOTING
                    SoundSynthesizer.playWhistle()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("screen_penalty")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header & Score Tracker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onExit, modifier = Modifier.testTag("btn_penalty_back")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "PENALTY SHOOTOUT (ROUND $currentRound)",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scoreboard Dots Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User side
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = userTeam.flagEmoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = userTeam.name, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(maxOf(5, currentRound)) { i ->
                                val res = userResults.getOrNull(i)
                                ShootoutDot(result = res)
                            }
                        }
                    }

                    // Score
                    Text(
                        text = "${userResults.count { it }} - ${compResults.count { it }}",
                        color = Color(0xFFFFD54F),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Comp side
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = compTeam.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = compTeam.flagEmoji, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(maxOf(5, currentRound)) { i ->
                                val res = compResults.getOrNull(i)
                                ShootoutDot(result = res)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal View & Interactive Aiming Area
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1B5E20))
            ) {
                val boxWidth = constraints.maxWidth.toFloat()
                val boxHeight = constraints.maxHeight.toFloat()

                val goalMarginX = boxWidth * 0.12f
                val goalMarginY = boxHeight * 0.15f
                val goalW = boxWidth * 0.76f
                val goalH = boxHeight * 0.55f

                // Stadium Goal Rendering
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Grass pitch stripes
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                        )
                    )

                    // Goal Net Background
                    drawRect(
                        color = Color(0x33000000),
                        topLeft = Offset(goalMarginX, goalMarginY),
                        size = Size(goalW, goalH)
                    )

                    // Goal Frame Posts
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(goalMarginX, goalMarginY),
                        size = Size(goalW, goalH),
                        style = Stroke(width = 6.dp.toPx())
                    )

                    // Net Mesh Grid Lines
                    for (i in 1..8) {
                        val gx = goalMarginX + (goalW / 9f) * i
                        drawLine(
                            color = Color(0x44FFFFFF),
                            start = Offset(gx, goalMarginY),
                            end = Offset(gx, goalMarginY + goalH),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (j in 1..5) {
                        val gy = goalMarginY + (goalH / 6f) * j
                        drawLine(
                            color = Color(0x44FFFFFF),
                            start = Offset(goalMarginX, gy),
                            end = Offset(goalMarginX + goalW, gy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Goalkeeper Rendering
                    val keeperTarget = keeperDiveZone ?: GoalZone.CENTER
                    val keeperX = goalMarginX + goalW * keeperTarget.xFrac
                    val keeperY = goalMarginY + goalH * keeperTarget.yFrac

                    drawCircle(
                        color = Color(0xFFFF9800),
                        radius = 24.dp.toPx(),
                        center = Offset(keeperX, keeperY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 24.dp.toPx(),
                        center = Offset(keeperX, keeperY),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Ball Rendering during shot
                    if (shotTargetZone != null) {
                        val ballX = goalMarginX + goalW * shotTargetZone!!.xFrac
                        val ballY = goalMarginY + goalH * shotTargetZone!!.yFrac
                        drawCircle(
                            color = Color.White,
                            radius = 16.dp.toPx(),
                            center = Offset(ballX, ballY)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 6.dp.toPx(),
                            center = Offset(ballX, ballY)
                        )
                    } else {
                        // Penalty Spot ball
                        val spotX = boxWidth / 2f
                        val spotY = boxHeight * 0.88f
                        drawCircle(
                            color = Color.White,
                            radius = 16.dp.toPx(),
                            center = Offset(spotX, spotY)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 6.dp.toPx(),
                            center = Offset(spotX, spotY)
                        )
                    }
                }

                // Interactive Aim Buttons on Goal
                if (!isAnimatingShot && !isGameOver) {
                    val density = LocalDensity.current
                    val halfBtnPx = with(density) { 27.dp.toPx() }

                    GoalZone.values().forEach { zone ->
                        val posX = goalMarginX + goalW * zone.xFrac
                        val posY = goalMarginY + goalH * zone.yFrac

                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        (posX - halfBtnPx).roundToInt(),
                                        (posY - halfBtnPx).roundToInt()
                                    )
                                }
                                .testTag("target_zone_${zone.name.lowercase()}"),
                            shape = CircleShape,
                            color = Color(0x77000000),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)),
                            onClick = {
                                if (currentTurn == ShootoutTurn.USER_SHOOTING) {
                                    // User kicks at zone
                                    val compGkChoice = GoalZone.values().random()
                                    resolveShot(shot = zone, keeper = compGkChoice, isUserShooting = true)
                                } else {
                                    // User dives as keeper
                                    val compStrikerShot = GoalZone.values().random()
                                    resolveShot(shot = compStrikerShot, keeper = zone, isUserShooting = false)
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (currentTurn == ShootoutTurn.USER_SHOOTING) "AIM" else "DIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Action banner announcement
                if (actionResultText.isNotEmpty() && isAnimatingShot) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xDD0F172A),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F))
                    ) {
                        Text(
                            text = actionResultText,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Turn Indicator
            Text(
                text = if (currentTurn == ShootoutTurn.USER_SHOOTING) {
                    "👉 YOUR TURN TO SHOOT: Tap a target in the goal!"
                } else {
                    "🧤 YOU ARE GOALKEEPER: Tap where to dive & save!"
                },
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Game Over Dialog
            if (isGameOver) {
                val userWon = userResults.count { it } > compResults.count { it }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (userWon) Color(0xFFFFD54F) else Color(0xFFFF5252))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (userWon) "🏆 SHOOTOUT VICTORY!" else "SHOOTOUT DEFEAT",
                            color = if (userWon) Color(0xFFFFD54F) else Color(0xFFFF5252),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    userResults.clear()
                                    compResults.clear()
                                    currentRound = 1
                                    currentTurn = ShootoutTurn.USER_SHOOTING
                                    isGameOver = false
                                    actionResultText = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = onExit) {
                                Text("MENU", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShootoutDot(result: Boolean?) {
    Surface(
        modifier = Modifier.size(18.dp),
        shape = CircleShape,
        color = when (result) {
            true -> Color(0xFF4CAF50)
            false -> Color(0xFFE53935)
            null -> Color(0xFF475569)
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (result == true) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            } else if (result == false) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}
