package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.model.AiDifficulty
import com.example.model.DefaultTeams
import com.example.model.GameMode
import com.example.model.MatchConfig
import com.example.model.Team
import com.example.ui.screens.FreeKickScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MatchScreen
import com.example.ui.screens.PenaltyShootoutScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    MAIN_MENU,
    MATCH,
    TOURNAMENT,
    PENALTY_SHOOTOUT,
    FREE_KICK
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FootballGameApp()
                }
            }
        }
    }
}

@Composable
fun FootballGameApp() {
    var currentScreen by remember { mutableStateOf(ScreenState.MAIN_MENU) }
    var currentMatchConfig by remember {
        mutableStateOf(
            MatchConfig(
                userTeam = DefaultTeams.RED_STRIKERS,
                compTeam = DefaultTeams.BLUE_TITANS,
                difficulty = AiDifficulty.MEDIUM,
                durationSeconds = 90
            )
        )
    }

    var tournamentUserTeam by remember { mutableStateOf(DefaultTeams.RED_STRIKERS) }
    var tournamentDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var tournamentMatchCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    var penaltyUserTeam by remember { mutableStateOf(DefaultTeams.RED_STRIKERS) }
    var penaltyCompTeam by remember { mutableStateOf(DefaultTeams.BLUE_TITANS) }
    var penaltyDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }

    var freeKickUserTeam by remember { mutableStateOf(DefaultTeams.RED_STRIKERS) }
    var freeKickCompTeam by remember { mutableStateOf(DefaultTeams.BLUE_TITANS) }

    when (currentScreen) {
        ScreenState.MAIN_MENU -> {
            MainMenuScreen(
                onStartQuickMatch = { config ->
                    currentMatchConfig = config
                    currentScreen = ScreenState.MATCH
                },
                onStartTournament = { userTeam, diff ->
                    tournamentUserTeam = userTeam
                    tournamentDifficulty = diff
                    currentScreen = ScreenState.TOURNAMENT
                },
                onStartPenaltyShootout = { uTeam, cTeam, diff ->
                    penaltyUserTeam = uTeam
                    penaltyCompTeam = cTeam
                    penaltyDifficulty = diff
                    currentScreen = ScreenState.PENALTY_SHOOTOUT
                },
                onStartFreeKick = { uTeam, cTeam ->
                    freeKickUserTeam = uTeam
                    freeKickCompTeam = cTeam
                    currentScreen = ScreenState.FREE_KICK
                }
            )
        }

        ScreenState.MATCH -> {
            MatchScreen(
                config = currentMatchConfig,
                onExitMatch = {
                    if (currentMatchConfig.mode == GameMode.TOURNAMENT) {
                        currentScreen = ScreenState.TOURNAMENT
                    } else {
                        currentScreen = ScreenState.MAIN_MENU
                    }
                },
                onRematch = {
                    // Triggers reload of match screen
                    val refreshedCfg = currentMatchConfig.copy()
                    currentMatchConfig = refreshedCfg
                }
            )
        }

        ScreenState.TOURNAMENT -> {
            TournamentScreen(
                userTeam = tournamentUserTeam,
                difficulty = tournamentDifficulty,
                onBackToMenu = { currentScreen = ScreenState.MAIN_MENU },
                onLaunchTournamentMatch = { matchCfg, onComplete ->
                    currentMatchConfig = matchCfg
                    tournamentMatchCallback = onComplete
                    currentScreen = ScreenState.MATCH
                }
            )
        }

        ScreenState.PENALTY_SHOOTOUT -> {
            PenaltyShootoutScreen(
                userTeam = penaltyUserTeam,
                compTeam = penaltyCompTeam,
                difficulty = penaltyDifficulty,
                onExit = { currentScreen = ScreenState.MAIN_MENU }
            )
        }

        ScreenState.FREE_KICK -> {
            FreeKickScreen(
                userTeam = freeKickUserTeam,
                compTeam = freeKickCompTeam,
                onExit = { currentScreen = ScreenState.MAIN_MENU }
            )
        }
    }
}

