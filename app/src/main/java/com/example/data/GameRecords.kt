package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class CareerStats(
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val goalsScored: Int = 0,
    val goalsConceded: Int = 0,
    val tournamentsWon: Int = 0,
    val penaltyWins: Int = 0
) {
    val winRate: Int
        get() = if (matchesPlayed > 0) ((wins.toDouble() / matchesPlayed) * 100).toInt() else 0
}

data class MatchHistoryRecord(
    val userTeam: String,
    val compTeam: String,
    val userScore: Int,
    val compScore: Int,
    val difficulty: String,
    val timestamp: Long
)

class GameRecordsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("football_game_stats", Context.MODE_PRIVATE)

    fun getCareerStats(): CareerStats {
        return CareerStats(
            matchesPlayed = prefs.getInt("matches_played", 0),
            wins = prefs.getInt("wins", 0),
            losses = prefs.getInt("losses", 0),
            draws = prefs.getInt("draws", 0),
            goalsScored = prefs.getInt("goals_scored", 0),
            goalsConceded = prefs.getInt("goals_conceded", 0),
            tournamentsWon = prefs.getInt("tournaments_won", 0),
            penaltyWins = prefs.getInt("penalty_wins", 0)
        )
    }

    fun recordMatchResult(userGoals: Int, compGoals: Int) {
        val stats = getCareerStats()
        val isWin = userGoals > compGoals
        val isDraw = userGoals == compGoals
        val isLoss = userGoals < compGoals

        prefs.edit()
            .putInt("matches_played", stats.matchesPlayed + 1)
            .putInt("wins", stats.wins + (if (isWin) 1 else 0))
            .putInt("draws", stats.draws + (if (isDraw) 1 else 0))
            .putInt("losses", stats.losses + (if (isLoss) 1 else 0))
            .putInt("goals_scored", stats.goalsScored + userGoals)
            .putInt("goals_conceded", stats.goalsConceded + compGoals)
            .apply()
    }

    fun recordTournamentWin() {
        val count = prefs.getInt("tournaments_won", 0)
        prefs.edit().putInt("tournaments_won", count + 1).apply()
    }

    fun recordPenaltyWin() {
        val count = prefs.getInt("penalty_wins", 0)
        prefs.edit().putInt("penalty_wins", count + 1).apply()
    }
}
