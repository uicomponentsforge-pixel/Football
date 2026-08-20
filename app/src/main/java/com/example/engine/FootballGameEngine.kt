package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundSynthesizer
import com.example.model.AiDifficulty
import com.example.model.BallEntity
import com.example.model.MatchConfig
import com.example.model.MatchPhase
import com.example.model.MatchStatistics
import com.example.model.Particle
import com.example.model.PlayerEntity
import com.example.model.PlayerRole
import com.example.model.Team
import com.example.model.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FootballGameEngine(
    val config: MatchConfig,
    val onGoalScored: (isUserGoal: Boolean, scorerName: String) -> Unit = { _, _ -> },
    val onMatchEnded: (MatchStatistics) -> Unit = {}
) {
    // Pitch dimensions (Virtual coordinates)
    val pitchWidth = 960f
    val pitchHeight = 600f
    val goalTop = 220f
    val goalBottom = 380f
    val goalDepth = 40f
    val goalPostRadius = 7f

    var matchPhase = MatchPhase.KICKOFF
    var timeRemainingSeconds = config.durationSeconds.toFloat()
    var userScore = 0
    var compScore = 0
    val stats = MatchStatistics()

    // Entities
    val players = mutableListOf<PlayerEntity>()
    val ball = BallEntity(pos = Vec2(pitchWidth / 2f, pitchHeight / 2f))
    val particles = mutableListOf<Particle>()

    // Celebration & Events
    var celebrationTimer = 0f
    var celebrationMessage = ""
    var lastScorer: PlayerEntity? = null
    var isSuperShotActive = false
    var superShotTimer = 0f
    var userTeamKickoff = true

    // Controls state
    var joystickDirection = Vec2.Zero
    var sprintHeld = false
    var shootCharge = 0f
    var isChargingShoot = false

    // AI timing counters
    private var aiDecisionCounter = 0
    private var lastPassCooldownComp = 0

    init {
        setupTeams()
        resetPositions(userKickoff = true)
    }

    private fun setupTeams() {
        players.clear()
        val uTeam = config.userTeam
        val cTeam = config.compTeam

        // User Players (5v5)
        players.add(PlayerEntity(1, uTeam.id, true, PlayerRole.GOALKEEPER, 1, "GK Keeper", Vec2(70f, pitchHeight / 2f)))
        players.add(PlayerEntity(2, uTeam.id, true, PlayerRole.DEFENDER, 3, "DF Miller", Vec2(220f, 180f)))
        players.add(PlayerEntity(3, uTeam.id, true, PlayerRole.DEFENDER, 4, "DF Davis", Vec2(220f, 420f)))
        players.add(PlayerEntity(4, uTeam.id, true, PlayerRole.MIDFIELDER, 8, "MF Silva", Vec2(350f, pitchHeight / 2f)))
        players.add(PlayerEntity(5, uTeam.id, true, PlayerRole.STRIKER, 10, "FW Leo", Vec2(440f, pitchHeight / 2f)))

        // Computer Players (5v5)
        players.add(PlayerEntity(6, cTeam.id, false, PlayerRole.GOALKEEPER, 1, "GK Khan", Vec2(pitchWidth - 70f, pitchHeight / 2f)))
        players.add(PlayerEntity(7, cTeam.id, false, PlayerRole.DEFENDER, 2, "DF Ramos", Vec2(pitchWidth - 220f, 180f)))
        players.add(PlayerEntity(8, cTeam.id, false, PlayerRole.DEFENDER, 5, "DF Van Dijk", Vec2(pitchWidth - 220f, 420f)))
        players.add(PlayerEntity(9, cTeam.id, false, PlayerRole.MIDFIELDER, 7, "MF Luka", Vec2(pitchWidth - 350f, pitchHeight / 2f)))
        players.add(PlayerEntity(10, cTeam.id, false, PlayerRole.STRIKER, 9, "FW Kane", Vec2(pitchWidth - 440f, pitchHeight / 2f)))

        // Assign base positions
        players.forEach {
            it.baseFormationPos = it.pos
            it.targetPos = it.pos
            it.facingAngle = if (it.isUserTeam) 0f else PI.toFloat()
        }

        // Default controlled player is Striker
        selectControlledPlayer(players.find { it.isUserTeam && it.role == PlayerRole.STRIKER } ?: players.first { it.isUserTeam })
    }

    fun resetPositions(userKickoff: Boolean) {
        userTeamKickoff = userKickoff
        ball.pos = Vec2(pitchWidth / 2f, pitchHeight / 2f)
        ball.vel = Vec2.Zero
        ball.z = 0f
        ball.vz = 0f
        ball.trail.clear()
        isSuperShotActive = false

        players.forEach { p ->
            p.vel = Vec2.Zero
            p.pos = p.baseFormationPos
            p.targetPos = p.baseFormationPos
            p.isStunned = 0
            p.slideTimer = 0
            p.stamina = 100f
            p.facingAngle = if (p.isUserTeam) 0f else PI.toFloat()
        }

        // Position kickoff taker near center
        if (userKickoff) {
            val striker = players.find { it.isUserTeam && it.role == PlayerRole.STRIKER }
            striker?.pos = Vec2(pitchWidth / 2f - 22f, pitchHeight / 2f)
            selectControlledPlayer(striker)
        } else {
            val compStriker = players.find { !it.isUserTeam && it.role == PlayerRole.STRIKER }
            compStriker?.pos = Vec2(pitchWidth / 2f + 22f, pitchHeight / 2f)
            val userMid = players.find { it.isUserTeam && it.role == PlayerRole.MIDFIELDER }
            selectControlledPlayer(userMid)
        }

        matchPhase = MatchPhase.KICKOFF
    }

    private fun selectControlledPlayer(player: PlayerEntity?) {
        players.filter { it.isUserTeam }.forEach { it.isControlled = false }
        player?.isControlled = true
    }

    fun getControlledPlayer(): PlayerEntity? = players.find { it.isUserTeam && it.isControlled }

    /** Main 60 FPS update tick */
    fun update(dt: Float = 1f / 60f) {
        if (matchPhase == MatchPhase.PAUSED) return

        if (matchPhase == MatchPhase.GOAL_CELEBRATION) {
            celebrationTimer -= dt
            updateParticles(dt)
            if (celebrationTimer <= 0f) {
                if (timeRemainingSeconds <= 0f) {
                    endMatch()
                } else {
                    resetPositions(userKickoff = !userTeamKickoff)
                    SoundSynthesizer.playWhistle()
                }
            }
            return
        }

        if (matchPhase == MatchPhase.FULL_TIME) {
            updateParticles(dt)
            return
        }

        // Match Clock
        timeRemainingSeconds -= dt
        if (timeRemainingSeconds <= 0f) {
            timeRemainingSeconds = 0f
            endMatch()
            return
        }

        // Handle shoot charging
        if (isChargingShoot) {
            shootCharge = (shootCharge + dt * 1.6f).coerceAtMost(1f)
        }

        // Track possession
        val userDist = players.filter { it.isUserTeam }.minOfOrNull { it.pos.distanceTo(ball.pos) } ?: 999f
        val compDist = players.filter { !it.isUserTeam }.minOfOrNull { it.pos.distanceTo(ball.pos) } ?: 999f
        if (userDist < 50f && userDist < compDist) {
            stats.userPossessionFrames++
        } else if (compDist < 50f && compDist < userDist) {
            stats.compPossessionFrames++
        }

        // Auto-switch controlled player to closest user outfield player
        val closestUser = players
            .filter { it.isUserTeam && it.role != PlayerRole.GOALKEEPER && it.isStunned == 0 }
            .minByOrNull { it.pos.distanceTo(ball.pos) }
        val currentCtrl = getControlledPlayer()
        if (closestUser != null && currentCtrl != null && closestUser.id != currentCtrl.id) {
            val distCurrent = currentCtrl.pos.distanceTo(ball.pos)
            val distClosest = closestUser.pos.distanceTo(ball.pos)
            if (distClosest < distCurrent - 75f) {
                selectControlledPlayer(closestUser)
            }
        }

        // Update User Controlled Player Movement
        updateUserPlayer(dt)

        // Update AI for Teammates & Computer
        updateTeammateAI(dt)
        updateComputerAI(dt)

        // Update Physics for all players
        players.forEach { p ->
            updatePlayerPhysics(p, dt)
        }

        // Update Ball Physics & collisions
        updateBallPhysics(dt)

        // Update Trail & Particles
        updateParticles(dt)
        updateBallTrail()

        // Check Goal Post Collisions & Goal Line Detection
        checkGoalConditions()

        if (isSuperShotActive) {
            superShotTimer -= dt
            if (superShotTimer <= 0f || ball.vel.length() < 8f) {
                isSuperShotActive = false
            }
        }
    }

    private fun updateUserPlayer(dt: Float) {
        val player = getControlledPlayer() ?: return
        if (player.isStunned > 0) return

        if (joystickDirection.length() > 0.05f) {
            val normDir = joystickDirection.normalized()
            val speedMult = if (sprintHeld && player.stamina > 10f) {
                player.stamina = (player.stamina - dt * 18f).coerceAtLeast(0f)
                1.45f
            } else {
                player.stamina = (player.stamina + dt * 8f).coerceAtMost(100f)
                1.0f
            }
            val targetSpeed = player.baseSpeed * speedMult
            player.vel = Vec2(
                player.vel.x * 0.75f + normDir.x * targetSpeed * 0.25f,
                player.vel.y * 0.75f + normDir.y * targetSpeed * 0.25f
            )
            player.facingAngle = normDir.angle()

            if (matchPhase == MatchPhase.KICKOFF) {
                matchPhase = MatchPhase.IN_PLAY
            }
        } else {
            player.vel = player.vel * 0.8f
            player.stamina = (player.stamina + dt * 12f).coerceAtMost(100f)
        }
    }

    private fun updateTeammateAI(dt: Float) {
        val userPlayers = players.filter { it.isUserTeam && !it.isControlled }
        val controlled = getControlledPlayer()

        userPlayers.forEach { p ->
            if (p.isStunned > 0) return@forEach

            when (p.role) {
                PlayerRole.GOALKEEPER -> {
                    // GK guards line between ball and goal
                    val targetY = (ball.pos.y).coerceIn(goalTop + 15f, goalBottom - 15f)
                    val targetX = 65f + (ball.pos.x / pitchWidth) * 35f
                    movePlayerTowards(p, Vec2(targetX, targetY), p.baseSpeed * 0.9f)
                    p.facingAngle = (ball.pos - p.pos).angle()
                }
                PlayerRole.DEFENDER -> {
                    // Hold defensive line, track computer attackers
                    val compAttackers = players.filter { !it.isUserTeam && it.pos.x < pitchWidth * 0.6f }
                    val threat = compAttackers.minByOrNull { it.pos.distanceTo(p.baseFormationPos) }
                    val target = if (threat != null && ball.pos.x < pitchWidth * 0.5f) {
                        Vec2(threat.pos.x - 25f, threat.pos.y).coerceInBounds(50f, pitchWidth * 0.55f, 40f, pitchHeight - 40f)
                    } else {
                        Vec2(p.baseFormationPos.x + (ball.pos.x - pitchWidth / 2f) * 0.2f, p.baseFormationPos.y)
                    }
                    movePlayerTowards(p, target, p.baseSpeed * 0.85f)
                    p.facingAngle = (ball.pos - p.pos).angle()
                }
                PlayerRole.MIDFIELDER, PlayerRole.STRIKER -> {
                    // Support attack, find open passing lane
                    val target = if (controlled != null && ball.pos.x > pitchWidth * 0.4f) {
                        Vec2(
                            p.baseFormationPos.x + 80f,
                            p.baseFormationPos.y + (if (p.pos.y < controlled.pos.y) -40f else 40f)
                        ).coerceInBounds(100f, pitchWidth - 90f, 50f, pitchHeight - 50f)
                    } else {
                        p.baseFormationPos + Vec2((ball.pos.x - pitchWidth / 2f) * 0.3f, (ball.pos.y - pitchHeight / 2f) * 0.2f)
                    }
                    movePlayerTowards(p, target, p.baseSpeed * 0.9f)
                    p.facingAngle = (ball.pos - p.pos).angle()
                }
            }
        }
    }

    private fun updateComputerAI(dt: Float) {
        val compPlayers = players.filter { !it.isUserTeam }
        val diff = config.difficulty
        val speedMult = diff.speedMultiplier
        aiDecisionCounter++

        if (lastPassCooldownComp > 0) lastPassCooldownComp--

        val compClosestToBall = compPlayers
            .filter { it.role != PlayerRole.GOALKEEPER && it.isStunned == 0 }
            .minByOrNull { it.pos.distanceTo(ball.pos) }

        compPlayers.forEach { p ->
            if (p.isStunned > 0) return@forEach

            when (p.role) {
                PlayerRole.GOALKEEPER -> {
                    // Smart GK positioning along arc
                    val targetY = ball.pos.y.coerceIn(goalTop + 15f, goalBottom - 15f)
                    val targetX = (pitchWidth - 65f) - ((pitchWidth - ball.pos.x) / pitchWidth) * 35f
                    movePlayerTowards(p, Vec2(targetX, targetY), p.baseSpeed * 0.95f * speedMult)
                    p.facingAngle = (ball.pos - p.pos).angle()

                    // Clear ball if very close
                    if (p.pos.distanceTo(ball.pos) < 32f && ball.z < 25f) {
                        aiClearBall(p)
                    }
                }
                else -> {
                    val isBallChaser = (p.id == compClosestToBall?.id)

                    if (isBallChaser) {
                        // Chase and contest ball
                        movePlayerTowards(p, ball.pos, p.baseSpeed * speedMult)
                        p.facingAngle = (ball.pos - p.pos).angle()

                        val distToBall = p.pos.distanceTo(ball.pos)
                        if (distToBall < 30f && ball.z < 25f) {
                            if (matchPhase == MatchPhase.KICKOFF) {
                                matchPhase = MatchPhase.IN_PLAY
                            }

                            // Has possession / in striking range!
                            val distToGoal = p.pos.distanceTo(Vec2(0f, pitchHeight / 2f))

                            // Check shoot condition
                            if (distToGoal < 340f && p.pos.x < pitchWidth * 0.45f) {
                                val shootChance = when (diff) {
                                    AiDifficulty.EASY -> 0.03f
                                    AiDifficulty.MEDIUM -> 0.07f
                                    AiDifficulty.HARD -> 0.12f
                                    AiDifficulty.MASTER -> 0.18f
                                }
                                if (Random.nextFloat() < shootChance) {
                                    aiShootAtGoal(p)
                                }
                            } else if (lastPassCooldownComp <= 0 && Random.nextFloat() < 0.04f) {
                                // Try passing to open forward
                                aiPassToTeammate(p)
                            }
                        } else if (distToBall < 55f && distToBall > 30f && p.slideCooldown <= 0) {
                            // Slide tackle if close to user with ball
                            val userWithBall = players.find { it.isUserTeam && it.pos.distanceTo(ball.pos) < 32f }
                            if (userWithBall != null && diff != AiDifficulty.EASY && Random.nextFloat() < 0.03f) {
                                executePlayerSlideTackle(p)
                            }
                        }
                    } else {
                        // Maintain formation & press
                        val target = when (p.role) {
                            PlayerRole.DEFENDER -> {
                                Vec2(
                                    p.baseFormationPos.x + (ball.pos.x - pitchWidth / 2f) * 0.25f,
                                    p.baseFormationPos.y
                                ).coerceInBounds(pitchWidth * 0.45f, pitchWidth - 80f, 40f, pitchHeight - 40f)
                            }
                            PlayerRole.MIDFIELDER -> {
                                Vec2(
                                    p.baseFormationPos.x + (ball.pos.x - pitchWidth / 2f) * 0.4f,
                                    p.baseFormationPos.y + (ball.pos.y - pitchHeight / 2f) * 0.3f
                                ).coerceInBounds(pitchWidth * 0.25f, pitchWidth - 100f, 50f, pitchHeight - 50f)
                            }
                            PlayerRole.STRIKER -> {
                                Vec2(
                                    (ball.pos.x - 120f).coerceIn(120f, pitchWidth * 0.6f),
                                    if (ball.pos.y > pitchHeight / 2f) 180f else 420f
                                )
                            }
                            else -> p.baseFormationPos
                        }
                        movePlayerTowards(p, target, p.baseSpeed * 0.85f * speedMult)
                        p.facingAngle = (ball.pos - p.pos).angle()
                    }
                }
            }
        }
    }

    private fun aiShootAtGoal(p: PlayerEntity) {
        val targetY = if (Random.nextBoolean()) goalTop + 25f else goalBottom - 25f
        val goalTarget = Vec2(0f, targetY)
        val dir = (goalTarget - p.pos).normalized()
        val power = when (config.difficulty) {
            AiDifficulty.EASY -> 11f + Random.nextFloat() * 2f
            AiDifficulty.MEDIUM -> 13f + Random.nextFloat() * 3f
            AiDifficulty.HARD -> 15f + Random.nextFloat() * 3.5f
            AiDifficulty.MASTER -> 17f + Random.nextFloat() * 4f
        }
        ball.vel = dir * power
        ball.vz = 2.5f + Random.nextFloat() * 2f
        ball.lastKickedByTeam = p.teamId
        ball.lastKickerId = p.id
        p.kickCooldown = 15
        stats.compShots++
        stats.compShotsOnTarget++
        SoundSynthesizer.playKick(0.85f)
        spawnKickParticles(p.pos, dir)
    }

    private fun aiPassToTeammate(p: PlayerEntity) {
        val teammates = players.filter { !it.isUserTeam && it.id != p.id && it.role != PlayerRole.GOALKEEPER }
        val best = teammates.minByOrNull { it.pos.x } ?: return // Pass to most forward
        val dir = (best.pos - p.pos).normalized()
        val dist = p.pos.distanceTo(best.pos)
        val speed = (dist * 0.045f).coerceIn(8f, 15f)
        ball.vel = dir * speed
        ball.vz = 1.2f
        ball.lastKickedByTeam = p.teamId
        ball.lastKickerId = p.id
        p.kickCooldown = 18
        lastPassCooldownComp = 45
        stats.compPasses++
        SoundSynthesizer.playKick(0.6f)
    }

    private fun aiClearBall(p: PlayerEntity) {
        val target = Vec2(pitchWidth / 2f, if (Random.nextBoolean()) 100f else pitchHeight - 100f)
        val dir = (target - p.pos).normalized()
        ball.vel = dir * 16f
        ball.vz = 4.5f
        ball.lastKickedByTeam = p.teamId
        ball.lastKickerId = p.id
        p.kickCooldown = 20
        SoundSynthesizer.playKick(0.9f)
    }

    private fun movePlayerTowards(p: PlayerEntity, target: Vec2, speed: Float) {
        val diff = target - p.pos
        if (diff.length() > 5f) {
            val dir = diff.normalized()
            p.vel = Vec2(
                p.vel.x * 0.8f + dir.x * speed * 0.2f,
                p.vel.y * 0.8f + dir.y * speed * 0.2f
            )
        } else {
            p.vel = p.vel * 0.7f
        }
    }

    private fun updatePlayerPhysics(p: PlayerEntity, dt: Float) {
        if (p.isStunned > 0) {
            p.isStunned--
            p.vel = p.vel * 0.85f
            p.pos = p.pos + p.vel
            return
        }

        if (p.slideTimer > 0) {
            p.slideTimer--
            // Slide tackle velocity burst
            p.pos = p.pos + p.vel
            spawnSlideParticles(p.pos)
            // Check tackling opponents
            val opponents = players.filter { it.isUserTeam != p.isUserTeam && it.isStunned == 0 }
            opponents.forEach { opp ->
                if (opp.pos.distanceTo(p.pos) < p.radius + opp.radius + 12f) {
                    opp.isStunned = 50 // Stun opponent!
                    opp.vel = p.vel * 0.5f
                    SoundSynthesizer.playTackle()
                    if (p.isUserTeam) stats.userTackles++ else stats.compTackles++

                    // Steal ball if close
                    if (ball.pos.distanceTo(opp.pos) < 30f) {
                        ball.vel = p.vel * 0.8f + Vec2(Random.nextFloat() * 4f - 2f, Random.nextFloat() * 4f - 2f)
                        ball.lastKickedByTeam = p.teamId
                        ball.lastKickerId = p.id
                    }
                }
            }
        } else {
            p.pos = p.pos + p.vel
        }

        if (p.slideCooldown > 0) p.slideCooldown--
        if (p.kickCooldown > 0) p.kickCooldown--

        // Pitch boundaries containment for players
        val minX = 25f
        val maxX = pitchWidth - 25f
        val minY = 25f
        val maxY = pitchHeight - 25f
        p.pos = Vec2(p.pos.x.coerceIn(minX, maxX), p.pos.y.coerceIn(minY, maxY))
    }

    private fun updateBallPhysics(dt: Float) {
        ball.pos = ball.pos + ball.vel

        // Grass friction & air drag
        val friction = if (ball.z <= 0.1f) 0.982f else 0.992f
        ball.vel = ball.vel * friction

        // Height & Gravity
        if (ball.z > 0f || ball.vz != 0f) {
            ball.z += ball.vz
            ball.vz -= 0.38f // Gravity
            if (ball.z <= 0f) {
                ball.z = 0f
                if (ball.vz < -1.5f) {
                    ball.vz = -ball.vz * 0.6f // Bounce
                    ball.vel = ball.vel * 0.92f
                } else {
                    ball.vz = 0f
                }
            }
        }

        // Top & Bottom boundary bounce
        if (ball.pos.y < ball.radius) {
            ball.pos = Vec2(ball.pos.x, ball.radius)
            ball.vel = Vec2(ball.vel.x * 0.85f, -ball.vel.y * 0.75f)
        } else if (ball.pos.y > pitchHeight - ball.radius) {
            ball.pos = Vec2(ball.pos.x, pitchHeight - ball.radius)
            ball.vel = Vec2(ball.vel.x * 0.85f, -ball.vel.y * 0.75f)
        }

        // Left & Right bounds (Except inside goal mouths)
        val inGoalY = ball.pos.y in (goalTop - 5f)..(goalBottom + 5f)

        if (!inGoalY) {
            if (ball.pos.x < ball.radius) {
                ball.pos = Vec2(ball.radius, ball.pos.y)
                ball.vel = Vec2(-ball.vel.x * 0.75f, ball.vel.y * 0.85f)
            } else if (ball.pos.x > pitchWidth - ball.radius) {
                ball.pos = Vec2(pitchWidth - ball.radius, ball.pos.y)
                ball.vel = Vec2(-ball.vel.x * 0.75f, ball.vel.y * 0.85f)
            }
        }

        // Player to Ball Dribble Interaction
        players.forEach { p ->
            if (p.isStunned > 0 || p.kickCooldown > 0) return@forEach
            val dist = p.pos.distanceTo(ball.pos)
            val touchRadius = p.radius + ball.radius + 4f

            if (dist < touchRadius && ball.z < 22f) {
                // Dribble touch: push ball along player motion
                val pushDir = (ball.pos - p.pos).let {
                    if (it.length() > 0.01f) it.normalized() else Vec2.fromAngle(p.facingAngle)
                }
                val dribbleSpeed = p.vel.length().coerceAtLeast(3f) + 1.2f
                ball.vel = pushDir * dribbleSpeed + p.vel * 0.4f
                ball.lastKickedByTeam = p.teamId
                ball.lastKickerId = p.id
            }
        }
    }

    private fun checkGoalConditions() {
        if (matchPhase != MatchPhase.IN_PLAY && matchPhase != MatchPhase.KICKOFF) return

        // Goal Post Collisions
        val leftTopPost = Vec2(0f, goalTop)
        val leftBottomPost = Vec2(0f, goalBottom)
        val rightTopPost = Vec2(pitchWidth, goalTop)
        val rightBottomPost = Vec2(pitchWidth, goalBottom)

        listOf(leftTopPost, leftBottomPost, rightTopPost, rightBottomPost).forEach { post ->
            if (ball.pos.distanceTo(post) < ball.radius + goalPostRadius) {
                val normal = (ball.pos - post).normalized()
                ball.vel = normal * (ball.vel.length() * 0.8f).coerceAtLeast(6f)
                ball.vz = 2.5f
                SoundSynthesizer.playPostHit()
                spawnPostHitParticles(post)
            }
        }

        // Goal scoring detection
        val insideGoalMouthY = ball.pos.y in (goalTop + 5f)..(goalBottom - 5f)

        // Computer Goal (Right goal scored by Player)
        if (ball.pos.x >= pitchWidth + 10f && insideGoalMouthY) {
            userScore++
            stats.userGoals++
            triggerGoalCelebration(isUserGoal = true)
        }
        // Player Goal (Left goal scored by Computer)
        else if (ball.pos.x <= -10f && insideGoalMouthY) {
            compScore++
            stats.compGoals++
            triggerGoalCelebration(isUserGoal = false)
        }
    }

    private fun triggerGoalCelebration(isUserGoal: Boolean) {
        matchPhase = MatchPhase.GOAL_CELEBRATION
        celebrationTimer = 3.2f
        val scorer = players.find { it.id == ball.lastKickerId }
        lastScorer = scorer

        celebrationMessage = if (isUserGoal) {
            if (isSuperShotActive) "⚡ ROCKET GOAL! ⚡" else "🎉 GOOOAL!! 🎉"
        } else {
            "⚽ OPPONENT SCORED!"
        }

        SoundSynthesizer.playGoalCheer()
        spawnGoalCelebrationConfetti(if (isUserGoal) Vec2(pitchWidth, pitchHeight / 2f) else Vec2(0f, pitchHeight / 2f))
        onGoalScored(isUserGoal, scorer?.name ?: (if (isUserGoal) config.userTeam.name else config.compTeam.name))
    }

    private fun endMatch() {
        matchPhase = MatchPhase.FULL_TIME
        SoundSynthesizer.playWhistle()
        onMatchEnded(stats)
    }

    // User Actions
    fun onShootPressed() {
        if (matchPhase != MatchPhase.IN_PLAY && matchPhase != MatchPhase.KICKOFF) return
        isChargingShoot = true
        shootCharge = 0f
    }

    fun onShootReleased() {
        if (!isChargingShoot) return
        isChargingShoot = false
        val player = getControlledPlayer() ?: return
        if (player.isStunned > 0) return

        val distToBall = player.pos.distanceTo(ball.pos)
        if (distToBall > player.radius + ball.radius + 28f || ball.z > 28f) return

        val power = shootCharge
        shootCharge = 0f

        // Target opponent goal
        val goalY = if (joystickDirection.y != 0f) {
            (pitchHeight / 2f + joystickDirection.y * 80f).coerceIn(goalTop + 15f, goalBottom - 15f)
        } else {
            if (Random.nextBoolean()) goalTop + 25f else goalBottom - 25f
        }
        val target = Vec2(pitchWidth, goalY)
        val dir = (target - player.pos).normalized()

        val isSuper = (power >= 0.88f)
        isSuperShotActive = isSuper
        if (isSuper) superShotTimer = 2.0f

        val shotSpeed = if (isSuper) 22f else 13f + power * 6.5f
        ball.vel = dir * shotSpeed
        ball.vz = 1.5f + power * 3f
        ball.lastKickedByTeam = player.teamId
        ball.lastKickerId = player.id
        player.kickCooldown = 15

        stats.userShots++
        stats.userShotsOnTarget++
        SoundSynthesizer.playKick(if (isSuper) 1.0f else 0.8f)
        spawnKickParticles(player.pos, dir)

        if (matchPhase == MatchPhase.KICKOFF) matchPhase = MatchPhase.IN_PLAY
    }

    fun performPass() {
        val player = getControlledPlayer() ?: return
        if (player.isStunned > 0) return
        val distToBall = player.pos.distanceTo(ball.pos)
        if (distToBall > player.radius + ball.radius + 32f || ball.z > 28f) return

        // Find best teammate in facing hemisphere
        val teammates = players.filter { it.isUserTeam && it.id != player.id && it.role != PlayerRole.GOALKEEPER }
        val facingVec = Vec2.fromAngle(player.facingAngle)

        val bestTeammate = teammates.maxByOrNull { tm ->
            val toTeammate = (tm.pos - player.pos).normalized()
            val dot = facingVec.x * toTeammate.x + facingVec.y * toTeammate.y
            dot * 2.0f - (player.pos.distanceTo(tm.pos) * 0.002f)
        } ?: teammates.firstOrNull() ?: return

        val dir = (bestTeammate.pos - player.pos).normalized()
        val dist = player.pos.distanceTo(bestTeammate.pos)
        val passSpeed = (dist * 0.052f).coerceIn(10f, 17f)

        ball.vel = dir * passSpeed
        ball.vz = 0.8f
        ball.lastKickedByTeam = player.teamId
        ball.lastKickerId = player.id
        player.kickCooldown = 14
        stats.userPasses++

        SoundSynthesizer.playKick(0.65f)
        selectControlledPlayer(bestTeammate)
        if (matchPhase == MatchPhase.KICKOFF) matchPhase = MatchPhase.IN_PLAY
    }

    fun performLob() {
        val player = getControlledPlayer() ?: return
        if (player.isStunned > 0) return
        val distToBall = player.pos.distanceTo(ball.pos)
        if (distToBall > player.radius + ball.radius + 30f) return

        val dir = Vec2.fromAngle(player.facingAngle)
        ball.vel = dir * 12.5f
        ball.vz = 5.5f // High lob arch!
        ball.lastKickedByTeam = player.teamId
        ball.lastKickerId = player.id
        player.kickCooldown = 18

        SoundSynthesizer.playKick(0.7f)
        if (matchPhase == MatchPhase.KICKOFF) matchPhase = MatchPhase.IN_PLAY
    }

    fun executePlayerSlideTackle(player: PlayerEntity) {
        if (player.isStunned > 0 || player.slideCooldown > 0) return
        player.slideTimer = 18
        player.slideCooldown = 60
        val dir = Vec2.fromAngle(player.facingAngle)
        player.vel = dir * (player.baseSpeed * 2.4f)
        SoundSynthesizer.playTackle()
    }

    fun performSlideTackle() {
        val player = getControlledPlayer() ?: return
        if (player.isStunned > 0 || player.slideCooldown > 0) return

        player.slideTimer = 18
        player.slideCooldown = 60
        val dir = if (joystickDirection.length() > 0.1f) joystickDirection.normalized() else Vec2.fromAngle(player.facingAngle)
        player.vel = dir * (player.baseSpeed * 2.4f)
        SoundSynthesizer.playTackle()
    }

    // Particle & VFX Helpers
    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.pos = p.pos + p.vel
            p.vel = p.vel * 0.94f
            p.life -= dt
            if (p.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun updateBallTrail() {
        if (ball.vel.length() > 4f) {
            ball.trail.add(0, ball.pos)
            if (ball.trail.size > 8) {
                ball.trail.removeAt(ball.trail.size - 1)
            }
        } else if (ball.trail.isNotEmpty()) {
            ball.trail.removeAt(ball.trail.size - 1)
        }
    }

    private fun spawnKickParticles(origin: Vec2, dir: Vec2) {
        repeat(8) {
            val speed = Random.nextFloat() * 4f + 2f
            val angle = dir.angle() + (Random.nextFloat() - 0.5f) * 1.2f
            particles.add(
                Particle(
                    pos = origin + dir * 15f,
                    vel = Vec2.fromAngle(angle, speed),
                    color = Color(0xFFFFD54F),
                    size = Random.nextFloat() * 4f + 3f,
                    life = 0.3f,
                    maxLife = 0.3f
                )
            )
        }
    }

    private fun spawnSlideParticles(pos: Vec2) {
        repeat(3) {
            particles.add(
                Particle(
                    pos = pos + Vec2(Random.nextFloat() * 16f - 8f, Random.nextFloat() * 16f - 8f),
                    vel = Vec2(Random.nextFloat() * 3f - 1.5f, Random.nextFloat() * 3f - 1.5f),
                    color = Color(0x88FFFFFF),
                    size = Random.nextFloat() * 5f + 3f,
                    life = 0.25f,
                    maxLife = 0.25f
                )
            )
        }
    }

    private fun spawnPostHitParticles(post: Vec2) {
        repeat(12) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            particles.add(
                Particle(
                    pos = post,
                    vel = Vec2.fromAngle(angle, Random.nextFloat() * 5f + 2f),
                    color = Color(0xFFFFEB3B),
                    size = Random.nextFloat() * 4f + 2f,
                    life = 0.4f,
                    maxLife = 0.4f
                )
            )
        }
    }

    private fun spawnGoalCelebrationConfetti(goalPos: Vec2) {
        val colors = listOf(
            Color(0xFFFFD54F), Color(0xFFFF1744), Color(0xFF00E5FF),
            Color(0xFF76FF03), Color(0xFFFF4081), Color(0xFFFFFFFF)
        )
        repeat(60) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 12f + 4f
            particles.add(
                Particle(
                    pos = goalPos + Vec2(Random.nextFloat() * 40f - 20f, Random.nextFloat() * 100f - 50f),
                    vel = Vec2.fromAngle(angle, speed),
                    color = colors.random(),
                    size = Random.nextFloat() * 7f + 4f,
                    life = Random.nextFloat() * 2.0f + 1.0f,
                    maxLife = 2.5f
                )
            )
        }
    }

    private fun Vec2.coerceInBounds(minX: Float, maxX: Float, minY: Float, maxY: Float): Vec2 {
        return Vec2(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
    }
}
