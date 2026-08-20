package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRecordsRepository
import com.example.engine.FootballGameEngine
import com.example.model.BallEntity
import com.example.model.MatchConfig
import com.example.model.MatchPhase
import com.example.model.MatchStatistics
import com.example.model.PlayerEntity
import com.example.model.PlayerRole
import com.example.model.StadiumType
import com.example.model.Vec2
import com.example.ui.components.GameActionControls
import com.example.ui.components.VirtualJoystick
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MatchScreen(
    config: MatchConfig,
    onExitMatch: () -> Unit,
    onRematch: () -> Unit
) {
    val context = LocalContext.current
    val recordsRepo = remember { GameRecordsRepository(context) }

    var isPaused by remember { mutableStateOf(false) }
    var showEndMatchDialog by remember { mutableStateOf(false) }

    val engine = remember {
        FootballGameEngine(
            config = config,
            onGoalScored = { isUserGoal, scorerName ->
                // Goal celebration handled in engine
            },
            onMatchEnded = { stats ->
                recordsRepo.recordMatchResult(stats.userGoals, stats.compGoals)
                showEndMatchDialog = true
            }
        )
    }

    var tick by remember { mutableStateOf(0L) }

    // Game loop running at 60 FPS
    LaunchedEffect(isPaused) {
        var lastTime = System.nanoTime()
        while (!isPaused && engine.matchPhase != MatchPhase.FULL_TIME) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                lastTime = now
                engine.update(dt)
                tick = now
            }
        }
    }

    BackHandler {
        if (showEndMatchDialog) {
            onExitMatch()
        } else {
            isPaused = !isPaused
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("screen_match")
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Match field scale factors (mapping 960x600 virtual coordinates to screen)
        val scaleX = screenWidth / engine.pitchWidth
        val scaleY = screenHeight / engine.pitchHeight
        val fieldScale = minOf(scaleX, scaleY)
        val offsetX = (screenWidth - (engine.pitchWidth * fieldScale)) / 2f
        val offsetY = (screenHeight - (engine.pitchHeight * fieldScale)) / 2f

        // Pitch & Game World Canvas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Draw pitch background grass mow stripes
            drawPitch(
                engine = engine,
                scale = fieldScale,
                offsetX = offsetX,
                offsetY = offsetY,
                stadium = config.stadium
            )

            // Draw goal nets
            drawGoals(
                engine = engine,
                scale = fieldScale,
                offsetX = offsetX,
                offsetY = offsetY
            )

            // Draw pitch lines & markings
            drawPitchMarkings(
                engine = engine,
                scale = fieldScale,
                offsetX = offsetX,
                offsetY = offsetY
            )

            // Draw particles & celebration effects
            engine.particles.forEach { p ->
                val px = offsetX + p.pos.x * fieldScale
                val py = offsetY + p.pos.y * fieldScale
                drawCircle(
                    color = p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)),
                    radius = p.size * fieldScale,
                    center = Offset(px, py)
                )
            }

            // Draw Players
            engine.players.forEach { player ->
                drawPlayer(
                    player = player,
                    engine = engine,
                    scale = fieldScale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    userColor = config.userTeam.primaryColor,
                    compColor = config.compTeam.primaryColor
                )
            }

            // Draw Ball
            drawBall(
                ball = engine.ball,
                engine = engine,
                scale = fieldScale,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }

        // Top Scoreboard HUD
        TopScoreboardHUD(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            config = config,
            userScore = engine.userScore,
            compScore = engine.compScore,
            timeRemaining = engine.timeRemainingSeconds.toInt(),
            onPauseClick = { isPaused = true }
        )

        // Radar Mini-Map in top left
        RadarMiniMap(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 70.dp, start = 16.dp),
            engine = engine
        )

        // Goal Celebration Overlay
        AnimatedVisibility(
            visible = engine.matchPhase == MatchPhase.GOAL_CELEBRATION,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GoalCelebrationBanner(
                message = engine.celebrationMessage,
                scorerName = engine.lastScorer?.name ?: "Scorer",
                isSuperShot = engine.isSuperShotActive
            )
        }

        // In-game On-screen Controls (Joystick & Action Buttons)
        if (engine.matchPhase == MatchPhase.IN_PLAY || engine.matchPhase == MatchPhase.KICKOFF) {
            // Left Joystick
            VirtualJoystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 28.dp),
                radiusDp = 64f,
                onDirectionChange = { dir ->
                    engine.joystickDirection = dir
                }
            )

            // Right Action Buttons
            GameActionControls(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
                isChargingShoot = engine.isChargingShoot,
                shootCharge = engine.shootCharge,
                stamina = engine.getControlledPlayer()?.stamina ?: 100f,
                onShootPress = { engine.onShootPressed() },
                onShootRelease = { engine.onShootReleased() },
                onPassClick = { engine.performPass() },
                onLobClick = { engine.performLob() },
                onSlideTackleClick = { engine.performSlideTackle() },
                onSprintToggle = { isSprint -> engine.sprintHeld = isSprint }
            )
        }

        // Pause Modal
        if (isPaused && !showEndMatchDialog) {
            PauseMenuDialog(
                stats = engine.stats,
                config = config,
                onResume = { isPaused = false },
                onRestart = {
                    isPaused = false
                    onRematch()
                },
                onExit = onExitMatch
            )
        }

        // Match Ended Summary Dialog
        if (showEndMatchDialog || engine.matchPhase == MatchPhase.FULL_TIME) {
            MatchSummaryDialog(
                stats = engine.stats,
                config = config,
                userWon = engine.userScore > engine.compScore,
                isDraw = engine.userScore == engine.compScore,
                onRematch = onRematch,
                onMainMenu = onExitMatch
            )
        }
    }
}

// -------------------------------------------------------------
// Pitch & Entity Drawing Implementations
// -------------------------------------------------------------

private fun DrawScope.drawPitch(
    engine: FootballGameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    stadium: StadiumType
) {
    val pW = engine.pitchWidth * scale
    val pH = engine.pitchHeight * scale

    // Base pitch grass color
    drawRect(
        color = stadium.grassColorDark,
        topLeft = Offset(offsetX, offsetY),
        size = Size(pW, pH)
    )

    // Mow stripes (Alternating vertical green strips)
    val numStripes = 10
    val stripeWidth = pW / numStripes
    for (i in 0 until numStripes step 2) {
        drawRect(
            color = stadium.grassColorLight,
            topLeft = Offset(offsetX + i * stripeWidth, offsetY),
            size = Size(stripeWidth, pH)
        )
    }

    // Outer Pitch boundary glow / border
    drawRect(
        color = Color(0x33FFFFFF),
        topLeft = Offset(offsetX, offsetY),
        size = Size(pW, pH),
        style = Stroke(width = 4.dp.toPx())
    )
}

private fun DrawScope.drawPitchMarkings(
    engine: FootballGameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val pW = engine.pitchWidth * scale
    val pH = engine.pitchHeight * scale
    val lineColor = Color(0xDDFFFFFF)
    val strokeWidth = 2.5f.dp.toPx()

    // Halfway center line
    drawLine(
        color = lineColor,
        start = Offset(offsetX + pW / 2f, offsetY),
        end = Offset(offsetX + pW / 2f, offsetY + pH),
        strokeWidth = strokeWidth
    )

    // Center circle
    drawCircle(
        color = lineColor,
        radius = 70f * scale,
        center = Offset(offsetX + pW / 2f, offsetY + pH / 2f),
        style = Stroke(width = strokeWidth)
    )

    // Center spot
    drawCircle(
        color = lineColor,
        radius = 4f * scale,
        center = Offset(offsetX + pW / 2f, offsetY + pH / 2f)
    )

    // Left Penalty Box
    val penW = 140f * scale
    val penH = 260f * scale
    drawRect(
        color = lineColor,
        topLeft = Offset(offsetX, offsetY + (pH - penH) / 2f),
        size = Size(penW, penH),
        style = Stroke(width = strokeWidth)
    )

    // Left Goal Area (6-yard box)
    val goalBoxW = 55f * scale
    val goalBoxH = 170f * scale
    drawRect(
        color = lineColor,
        topLeft = Offset(offsetX, offsetY + (pH - goalBoxH) / 2f),
        size = Size(goalBoxW, goalBoxH),
        style = Stroke(width = strokeWidth)
    )

    // Right Penalty Box
    drawRect(
        color = lineColor,
        topLeft = Offset(offsetX + pW - penW, offsetY + (pH - penH) / 2f),
        size = Size(penW, penH),
        style = Stroke(width = strokeWidth)
    )

    // Right Goal Area
    drawRect(
        color = lineColor,
        topLeft = Offset(offsetX + pW - goalBoxW, offsetY + (pH - goalBoxH) / 2f),
        size = Size(goalBoxW, goalBoxH),
        style = Stroke(width = strokeWidth)
    )

    // Corner Arcs
    val cornerR = 22f * scale
    drawArc(
        color = lineColor,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(offsetX - cornerR, offsetY - cornerR),
        size = Size(cornerR * 2, cornerR * 2),
        style = Stroke(width = strokeWidth)
    )
    drawArc(
        color = lineColor,
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(offsetX - cornerR, offsetY + pH - cornerR),
        size = Size(cornerR * 2, cornerR * 2),
        style = Stroke(width = strokeWidth)
    )
    drawArc(
        color = lineColor,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(offsetX + pW - cornerR, offsetY - cornerR),
        size = Size(cornerR * 2, cornerR * 2),
        style = Stroke(width = strokeWidth)
    )
    drawArc(
        color = lineColor,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(offsetX + pW - cornerR, offsetY + pH - cornerR),
        size = Size(cornerR * 2, cornerR * 2),
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawGoals(
    engine: FootballGameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val gTop = (offsetY + engine.goalTop * scale)
    val gBot = (offsetY + engine.goalBottom * scale)
    val gH = gBot - gTop
    val gD = engine.goalDepth * scale

    // Left Goal Net
    drawRect(
        color = Color(0x33FFFFFF),
        topLeft = Offset(offsetX - gD, gTop),
        size = Size(gD, gH)
    )
    drawRect(
        color = Color(0xFFE0E0E0),
        topLeft = Offset(offsetX - gD, gTop),
        size = Size(gD, gH),
        style = Stroke(width = 3.dp.toPx())
    )

    // Right Goal Net
    val rightGoalX = offsetX + engine.pitchWidth * scale
    drawRect(
        color = Color(0x33FFFFFF),
        topLeft = Offset(rightGoalX, gTop),
        size = Size(gD, gH)
    )
    drawRect(
        color = Color(0xFFE0E0E0),
        topLeft = Offset(rightGoalX, gTop),
        size = Size(gD, gH),
        style = Stroke(width = 3.dp.toPx())
    )
}

private val playerNumberPaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
}

private fun DrawScope.drawPlayer(
    player: PlayerEntity,
    engine: FootballGameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    userColor: Color,
    compColor: Color
) {
    val px = offsetX + player.pos.x * scale
    val py = offsetY + player.pos.y * scale
    val r = player.radius * scale

    // Player ground shadow
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(px - r * 1.1f, py - r * 0.5f + r * 0.4f),
        size = Size(r * 2.2f, r * 1.2f)
    )

    // User controlled halo & direction arrow
    if (player.isControlled) {
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = r * 1.55f,
            center = Offset(px, py),
            style = Stroke(width = 3.dp.toPx())
        )

        // Direction pointer arrow
        val dirX = cos(player.facingAngle) * (r * 1.9f)
        val dirY = sin(player.facingAngle) * (r * 1.9f)
        drawLine(
            color = Color(0xFFFFD54F),
            start = Offset(px, py),
            end = Offset(px + dirX, py + dirY),
            strokeWidth = 3.5f.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Slide tackle trail
    if (player.slideTimer > 0) {
        val slideBackX = px - cos(player.facingAngle) * (r * 2f)
        val slideBackY = py - sin(player.facingAngle) * (r * 2f)
        drawLine(
            color = Color(0x99FFFFFF),
            start = Offset(slideBackX, slideBackY),
            end = Offset(px, py),
            strokeWidth = r * 1.5f,
            cap = StrokeCap.Round
        )
    }

    // Jersey Body
    val jerseyColor = if (player.isUserTeam) {
        if (player.role == PlayerRole.GOALKEEPER) Color(0xFFFF9800) else userColor
    } else {
        if (player.role == PlayerRole.GOALKEEPER) Color(0xFF00BCD4) else compColor
    }

    // Outer collar / border
    drawCircle(
        color = Color.White,
        radius = r,
        center = Offset(px, py)
    )
    drawCircle(
        color = jerseyColor,
        radius = r * 0.88f,
        center = Offset(px, py)
    )

    // Player Number text
    drawContext.canvas.nativeCanvas.apply {
        playerNumberPaint.textSize = (r * 1.05f)
        drawText(
            "${player.number}",
            px,
            py + (r * 0.38f),
            playerNumberPaint
        )
    }

    // Stun indicator
    if (player.isStunned > 0) {
        drawCircle(
            color = Color(0xFFFFEB3B),
            radius = r * 1.3f,
            center = Offset(px, py),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawBall(
    ball: BallEntity,
    engine: FootballGameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val bx = offsetX + ball.pos.x * scale
    val by = offsetY + ball.pos.y * scale
    val r = ball.radius * scale
    val zOffset = ball.z * scale

    // Motion Trail
    ball.trail.forEachIndexed { index, pos ->
        val alpha = (1f - (index.toFloat() / ball.trail.size)) * 0.4f
        val tx = offsetX + pos.x * scale
        val ty = offsetY + pos.y * scale
        drawCircle(
            color = if (engine.isSuperShotActive) Color(0xFFFF5722).copy(alpha = alpha) else Color.White.copy(alpha = alpha),
            radius = r * (1f - index * 0.08f),
            center = Offset(tx, ty)
        )
    }

    // Ground Shadow (shrinks when high)
    val shadowScale = (1f - (ball.z / 120f)).coerceIn(0.4f, 1f)
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(bx - (r * 1.1f * shadowScale), by - (r * 0.6f * shadowScale) + 3f),
        size = Size(r * 2.2f * shadowScale, r * 1.2f * shadowScale)
    )

    // Ball Sphere (elevated by zOffset)
    val ballCenterY = by - zOffset

    if (engine.isSuperShotActive) {
        // Fireball glow
        drawCircle(
            color = Color(0xFFFF1744),
            radius = r * 1.45f,
            center = Offset(bx, ballCenterY)
        )
        drawCircle(
            color = Color(0xFFFFD600),
            radius = r * 1.15f,
            center = Offset(bx, ballCenterY)
        )
    }

    // Ball white sphere
    drawCircle(
        color = Color.White,
        radius = r,
        center = Offset(bx, ballCenterY)
    )

    // Pentagons & shading
    drawCircle(
        color = Color(0xFF212121),
        radius = r * 0.42f,
        center = Offset(bx, ballCenterY)
    )
    drawCircle(
        color = Color(0xFF424242),
        radius = r,
        center = Offset(bx, ballCenterY),
        style = Stroke(width = 1.5f.dp.toPx())
    )
}

// -------------------------------------------------------------
// UI Overlays & HUD
// -------------------------------------------------------------

@Composable
private fun TopScoreboardHUD(
    modifier: Modifier = Modifier,
    config: MatchConfig,
    userScore: Int,
    compScore: Int,
    timeRemaining: Int,
    onPauseClick: () -> Unit
) {
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    Card(
        modifier = modifier.testTag("scoreboard_hud"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Team
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = config.userTeam.flagEmoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = config.userTeam.shortName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Scores Display
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B)
            ) {
                Text(
                    text = "$userScore - $compScore",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            // Comp Team
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = config.compTeam.shortName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = config.compTeam.flagEmoji, fontSize = 20.sp)
            }

            // Match Clock
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (timeRemaining < 15) Color(0xFFD32F2F) else Color(0xFF334155)
            ) {
                Text(
                    text = timeFormatted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pause Button
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_pause")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RadarMiniMap(
    modifier: Modifier = Modifier,
    engine: FootballGameEngine
) {
    Card(
        modifier = modifier.size(width = 110.dp, height = 70.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA000000)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / engine.pitchWidth
            val scaleY = size.height / engine.pitchHeight

            // Halfway line
            drawLine(
                color = Color(0x44FFFFFF),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1f
            )

            // Players
            engine.players.forEach { p ->
                val px = p.pos.x * scaleX
                val py = p.pos.y * scaleY
                drawCircle(
                    color = if (p.isUserTeam) Color(0xFF00E5FF) else Color(0xFFFF1744),
                    radius = if (p.isControlled) 3.5f else 2.2f,
                    center = Offset(px, py)
                )
            }

            // Ball
            val bx = engine.ball.pos.x * scaleX
            val by = engine.ball.pos.y * scaleY
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 2.5f,
                center = Offset(bx, by)
            )
        }
    }
}

@Composable
private fun GoalCelebrationBanner(
    message: String,
    scorerName: String,
    isSuperShot: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuperShot) Color(0xFFD50000) else Color(0xFF1E88E5)
        ),
        elevation = CardDefaults.cardElevation(16.dp),
        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFD54F))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scored by $scorerName",
                color = Color(0xFFFFD54F),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PauseMenuDialog(
    stats: MatchStatistics,
    config: MatchConfig,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB000000))
            .testTag("dialog_pause"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(320.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MATCH PAUSED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Stats breakdown
                StatsRow(label = "Possession", userVal = "${stats.userPossessionPercent}%", compVal = "${stats.compPossessionPercent}%")
                StatsRow(label = "Shots (On Target)", userVal = "${stats.userShots} (${stats.userShotsOnTarget})", compVal = "${stats.compShots} (${stats.compShotsOnTarget})")
                StatsRow(label = "Tackles", userVal = "${stats.userTackles}", compVal = "${stats.compTackles}")
                StatsRow(label = "Passes", userVal = "${stats.userPasses}", compVal = "${stats.compPasses}")

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().testTag("btn_resume"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME MATCH", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().testTag("btn_restart")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTART", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().testTag("btn_exit_match")
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAIN MENU", color = Color(0xFFFF5252))
                }
            }
        }
    }
}

@Composable
private fun MatchSummaryDialog(
    stats: MatchStatistics,
    config: MatchConfig,
    userWon: Boolean,
    isDraw: Boolean,
    onRematch: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .testTag("dialog_match_summary"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(340.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (userWon) Color(0xFFFFD54F) else Color(0xFF475569))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        userWon -> "🏆 VICTORY!"
                        isDraw -> "🤝 DRAW MATCH"
                        else -> "DEFEAT"
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = when {
                        userWon -> Color(0xFFFFD54F)
                        isDraw -> Color(0xFF00E5FF)
                        else -> Color(0xFFFF5252)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Final Score Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = config.userTeam.shortName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${stats.userGoals} - ${stats.compGoals}",
                            color = Color(0xFFFFD54F),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(text = config.compTeam.shortName, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Match Stats Table
                StatsRow(label = "Possession", userVal = "${stats.userPossessionPercent}%", compVal = "${stats.compPossessionPercent}%")
                StatsRow(label = "Shots (On Target)", userVal = "${stats.userShots} (${stats.userShotsOnTarget})", compVal = "${stats.compShots} (${stats.compShotsOnTarget})")
                StatsRow(label = "Tackles Won", userVal = "${stats.userTackles}", compVal = "${stats.compTackles}")
                StatsRow(label = "Passes Completed", userVal = "${stats.userPasses}", compVal = "${stats.compPasses}")

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRematch,
                    modifier = Modifier.fillMaxWidth().testTag("btn_rematch"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth().testTag("btn_summary_main_menu")
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAIN MENU", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(label: String, userVal: String, compVal: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = userVal, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(text = compVal, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
