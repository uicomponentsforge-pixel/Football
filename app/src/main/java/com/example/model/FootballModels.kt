package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** 2D Vector math helper */
data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vec2(x / scalar, y / scalar) else Vec2(0f, 0f)

    fun length(): Float = sqrt(x * x + y * y)
    fun distanceTo(other: Vec2): Float = hypot(x - other.x, y - other.y)

    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0.0001f) Vec2(x / len, y / len) else Vec2(0f, 0f)
    }

    fun angle(): Float = atan2(y, x)

    companion object {
        val Zero = Vec2(0f, 0f)
        fun fromAngle(angleRad: Float, length: Float = 1f): Vec2 =
            Vec2(cos(angleRad) * length, sin(angleRad) * length)
    }
}

enum class PlayerRole {
    GOALKEEPER,
    DEFENDER,
    MIDFIELDER,
    STRIKER
}

enum class AiDifficulty(val title: String, val description: String, val speedMultiplier: Float, val reactionDelayFrames: Int) {
    EASY("Rookie", "Relaxed defense, slower shots", 0.78f, 18),
    MEDIUM("Pro", "Balanced passing & tactical marking", 0.92f, 10),
    HARD("Legend", "Aggressive pressing, sharp shots", 1.05f, 4),
    MASTER("World Class", "Relentless tiki-taka, rocket shots", 1.15f, 1)
}

enum class GameMode(val title: String, val subtitle: String) {
    QUICK_MATCH("Quick Match", "Single match vs AI"),
    TOURNAMENT("Championship Cup", "8-Team knockout tournament"),
    PENALTY_SHOOTOUT("Penalty Shootout", "High stakes shootout vs AI"),
    FREE_KICK("Free Kick Challenge", "Curve shots past the wall")
}

enum class MatchPhase {
    KICKOFF,
    IN_PLAY,
    GOAL_CELEBRATION,
    THROW_IN,
    CORNER_KICK,
    HALF_TIME,
    FULL_TIME,
    PAUSED
}

enum class StadiumType(val title: String, val grassColorDark: Color, val grassColorLight: Color, val lineStyle: String) {
    EMERALD_ARENA("Emerald Arena", Color(0xFF1B5E20), Color(0xFF2E7D32), "Classic"),
    SUNSET_STADIUM("Sunset Stadium", Color(0xFF264653), Color(0xFF2A9D8F), "Sunset"),
    CYBER_DOME("Cyber Dome", Color(0xFF0F172A), Color(0xFF1E293B), "Neon"),
    WINTER_PITCH("Winter Pitch", Color(0xFF334155), Color(0xFF475569), "Frost")
}

data class Team(
    val id: String,
    val name: String,
    val shortName: String,
    val flagEmoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val numberColor: Color = Color.White,
    val attackRating: Int = 85,
    val defenseRating: Int = 82,
    val speedRating: Int = 84
)

data class PlayerEntity(
    val id: Int,
    val teamId: String,
    val isUserTeam: Boolean,
    val role: PlayerRole,
    val number: Int,
    val name: String,
    var pos: Vec2,
    var vel: Vec2 = Vec2.Zero,
    var targetPos: Vec2 = pos,
    var baseFormationPos: Vec2 = pos,
    var facingAngle: Float = 0f,
    var baseSpeed: Float = 4.2f,
    var kickPower: Float = 14f,
    var radius: Float = 16f,
    var kickCooldown: Int = 0,
    var slideCooldown: Int = 0,
    var slideTimer: Int = 0,
    var isStunned: Int = 0,
    var isControlled: Boolean = false,
    var stamina: Float = 100f,
    var isSprinting: Boolean = false
)

data class BallEntity(
    var pos: Vec2,
    var vel: Vec2 = Vec2.Zero,
    var z: Float = 0f,
    var vz: Float = 0f,
    var radius: Float = 9f,
    var spin: Float = 0f,
    var lastKickedByTeam: String? = null,
    var lastKickerId: Int? = null,
    val trail: MutableList<Vec2> = mutableListOf()
)

data class Particle(
    var pos: Vec2,
    var vel: Vec2,
    var color: Color,
    var size: Float,
    var life: Float,
    var maxLife: Float
)

data class MatchStatistics(
    var userGoals: Int = 0,
    var compGoals: Int = 0,
    var userShots: Int = 0,
    var compShots: Int = 0,
    var userShotsOnTarget: Int = 0,
    var compShotsOnTarget: Int = 0,
    var userPasses: Int = 0,
    var compPasses: Int = 0,
    var userTackles: Int = 0,
    var compTackles: Int = 0,
    var userPossessionFrames: Long = 0,
    var compPossessionFrames: Long = 0
) {
    val userPossessionPercent: Int
        get() {
            val total = userPossessionFrames + compPossessionFrames
            return if (total > 0) ((userPossessionFrames.toDouble() / total) * 100).toInt() else 50
        }
    val compPossessionPercent: Int
        get() = 100 - userPossessionPercent
}

data class MatchConfig(
    val userTeam: Team,
    val compTeam: Team,
    val difficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val durationSeconds: Int = 90,
    val stadium: StadiumType = StadiumType.EMERALD_ARENA,
    val mode: GameMode = GameMode.QUICK_MATCH,
    val teamSize: Int = 5 // 5v5 mini arcade football
)

data class TournamentMatch(
    val round: Int, // 1: Quarter, 2: Semi, 3: Final
    val matchIndex: Int,
    val team1: Team,
    val team2: Team,
    var score1: Int? = null,
    var score2: Int? = null,
    var isCompleted: Boolean = false,
    var winner: Team? = null,
    val isUserMatch: Boolean = false
)

object DefaultTeams {
    val RED_STRIKERS = Team(
        id = "red_strikers",
        name = "Red Strikers",
        shortName = "RED",
        flagEmoji = "🔴",
        primaryColor = Color(0xFFD32F2F),
        secondaryColor = Color(0xFFFFCDD2),
        attackRating = 88,
        defenseRating = 80,
        speedRating = 85
    )
    val BLUE_TITANS = Team(
        id = "blue_titans",
        name = "Blue Titans",
        shortName = "BLU",
        flagEmoji = "🔵",
        primaryColor = Color(0xFF1976D2),
        secondaryColor = Color(0xFFBBDEFB),
        attackRating = 82,
        defenseRating = 90,
        speedRating = 80
    )
    val GOLD_LIONS = Team(
        id = "gold_lions",
        name = "Golden Lions",
        shortName = "GLD",
        flagEmoji = "🦁",
        primaryColor = Color(0xFFF57F17),
        secondaryColor = Color(0xFFFFF9C4),
        attackRating = 89,
        defenseRating = 84,
        speedRating = 87
    )
    val GREEN_DRAGONS = Team(
        id = "green_dragons",
        name = "Green Dragons",
        shortName = "GRN",
        flagEmoji = "🐉",
        primaryColor = Color(0xFF2E7D32),
        secondaryColor = Color(0xFFC8E6C9),
        attackRating = 84,
        defenseRating = 86,
        speedRating = 83
    )
    val CYBER_UNITED = Team(
        id = "cyber_united",
        name = "Cyber United",
        shortName = "CYB",
        flagEmoji = "⚡",
        primaryColor = Color(0xFF00ACC1),
        secondaryColor = Color(0xFFE0F7FA),
        attackRating = 91,
        defenseRating = 79,
        speedRating = 92
    )
    val ROYAL_PURPLE = Team(
        id = "royal_purple",
        name = "Royal Monarchs",
        shortName = "RYL",
        flagEmoji = "👑",
        primaryColor = Color(0xFF7B1FA2),
        secondaryColor = Color(0xFFE1BEE7),
        attackRating = 86,
        defenseRating = 88,
        speedRating = 84
    )
    val SOLAR_FLAME = Team(
        id = "solar_flame",
        name = "Solar Phoenix",
        shortName = "SOL",
        flagEmoji = "🔥",
        primaryColor = Color(0xFFE64A19),
        secondaryColor = Color(0xFFFFCCBC),
        attackRating = 93,
        defenseRating = 76,
        speedRating = 90
    )
    val SHADOW_KNIGHTS = Team(
        id = "shadow_knights",
        name = "Shadow Knights",
        shortName = "SHD",
        flagEmoji = "⚔️",
        primaryColor = Color(0xFF212121),
        secondaryColor = Color(0xFFBDBDBD),
        attackRating = 85,
        defenseRating = 92,
        speedRating = 82
    )

    val ALL_TEAMS = listOf(
        RED_STRIKERS,
        BLUE_TITANS,
        GOLD_LIONS,
        GREEN_DRAGONS,
        CYBER_UNITED,
        ROYAL_PURPLE,
        SOLAR_FLAME,
        SHADOW_KNIGHTS
    )
}
