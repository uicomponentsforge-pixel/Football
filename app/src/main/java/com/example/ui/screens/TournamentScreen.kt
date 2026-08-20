package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundSynthesizer
import com.example.data.GameRecordsRepository
import com.example.model.AiDifficulty
import com.example.model.DefaultTeams
import com.example.model.GameMode
import com.example.model.MatchConfig
import com.example.model.Team
import com.example.model.TournamentMatch
import kotlin.random.Random

@Composable
fun TournamentScreen(
    userTeam: Team,
    difficulty: AiDifficulty,
    onBackToMenu: () -> Unit,
    onLaunchTournamentMatch: (MatchConfig, onMatchComplete: (userWon: Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val recordsRepo = remember { GameRecordsRepository(context) }

    // 8-Team bracket initialization
    val remainingTeams = remember {
        DefaultTeams.ALL_TEAMS.filter { it.id != userTeam.id }.shuffled().take(7)
    }

    var currentRound by remember { mutableStateOf(1) } // 1: Quarter, 2: Semi, 3: Final, 4: Champion!
    var hasLostTournament by remember { mutableStateOf(false) }

    // Bracket matches
    var qfMatches by remember {
        mutableStateOf(
            listOf(
                TournamentMatch(1, 0, userTeam, remainingTeams[0], isUserMatch = true),
                TournamentMatch(1, 1, remainingTeams[1], remainingTeams[2]),
                TournamentMatch(1, 2, remainingTeams[3], remainingTeams[4]),
                TournamentMatch(1, 3, remainingTeams[5], remainingTeams[6])
            )
        )
    }

    var sfMatches by remember { mutableStateOf<List<TournamentMatch>>(emptyList()) }
    var finalMatch by remember { mutableStateOf<TournamentMatch?>(null) }

    BackHandler { onBackToMenu() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("screen_tournament")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToMenu,
                    modifier = Modifier.testTag("btn_tournament_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CHAMPIONSHIP CUP",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when (currentRound) {
                            1 -> "Quarter-Finals (8 Teams)"
                            2 -> "Semi-Finals (4 Teams)"
                            3 -> "CHAMPIONSHIP FINAL!"
                            else -> "CHAMPIONS OF THE CUP!"
                        },
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bracket rounds display
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Round 1: Quarterfinals
                item {
                    RoundCard(title = "QUARTER-FINALS", isCurrent = currentRound == 1) {
                        qfMatches.forEach { match ->
                            MatchFixtureItem(match = match)
                        }
                    }
                }

                // Round 2: Semifinals
                if (sfMatches.isNotEmpty()) {
                    item {
                        RoundCard(title = "SEMI-FINALS", isCurrent = currentRound == 2) {
                            sfMatches.forEach { match ->
                                MatchFixtureItem(match = match)
                            }
                        }
                    }
                }

                // Round 3: Final
                if (finalMatch != null) {
                    item {
                        RoundCard(title = "CUP FINAL", isCurrent = currentRound == 3) {
                            MatchFixtureItem(match = finalMatch!!)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button at Bottom
            if (hasLostTournament) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ELIMINATED FROM CUP",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBackToMenu,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("BACK TO MENU", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (currentRound == 4) {
                // Champion celebration!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF57F17))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                        Text(
                            text = "🏆 YOU WON THE CHAMPIONSHIP CUP! 🏆",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBackToMenu,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("CLAIM TROPHY & RETURN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Play Next Fixture Button
                Button(
                    onClick = {
                        val activeUserMatch = when (currentRound) {
                            1 -> qfMatches.find { it.isUserMatch }
                            2 -> sfMatches.find { it.isUserMatch }
                            3 -> if (finalMatch?.isUserMatch == true) finalMatch else null
                            else -> null
                        } ?: return@Button

                        val opponent = if (activeUserMatch.team1.id == userTeam.id) activeUserMatch.team2 else activeUserMatch.team1

                        val matchCfg = MatchConfig(
                            userTeam = userTeam,
                            compTeam = opponent,
                            difficulty = when (currentRound) {
                                1 -> difficulty
                                2 -> if (difficulty == AiDifficulty.EASY) AiDifficulty.MEDIUM else AiDifficulty.HARD
                                else -> AiDifficulty.MASTER
                            },
                            durationSeconds = 60,
                            mode = GameMode.TOURNAMENT
                        )

                        onLaunchTournamentMatch(matchCfg) { userWon ->
                            if (userWon) {
                                // Simulate other matches in round and advance
                                if (currentRound == 1) {
                                    val simQF = qfMatches.toMutableList()
                                    simQF[0] = simQF[0].copy(isCompleted = true, score1 = 3, score2 = 1, winner = userTeam)
                                    for (i in 1..3) {
                                        val s1 = Random.nextInt(0, 4)
                                        val s2 = if (s1 == 0) 1 else Random.nextInt(0, s1)
                                        simQF[i] = simQF[i].copy(
                                            isCompleted = true,
                                            score1 = s1,
                                            score2 = s2,
                                            winner = if (s1 >= s2) simQF[i].team1 else simQF[i].team2
                                        )
                                    }
                                    qfMatches = simQF
                                    sfMatches = listOf(
                                        TournamentMatch(2, 0, userTeam, simQF[1].winner!!, isUserMatch = true),
                                        TournamentMatch(2, 1, simQF[2].winner!!, simQF[3].winner!!)
                                    )
                                    currentRound = 2
                                } else if (currentRound == 2) {
                                    val simSF = sfMatches.toMutableList()
                                    simSF[0] = simSF[0].copy(isCompleted = true, score1 = 2, score2 = 0, winner = userTeam)
                                    val s1 = Random.nextInt(1, 4)
                                    val s2 = Random.nextInt(0, s1)
                                    simSF[1] = simSF[1].copy(isCompleted = true, score1 = s1, score2 = s2, winner = simSF[1].team1)
                                    sfMatches = simSF
                                    finalMatch = TournamentMatch(3, 0, userTeam, simSF[1].winner!!, isUserMatch = true)
                                    currentRound = 3
                                } else if (currentRound == 3) {
                                    finalMatch = finalMatch?.copy(isCompleted = true, score1 = 3, score2 = 2, winner = userTeam)
                                    currentRound = 4
                                    recordsRepo.recordTournamentWin()
                                    SoundSynthesizer.playGoalCheer()
                                }
                            } else {
                                hasLostTournament = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_play_tournament_match"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (currentRound) {
                            1 -> "PLAY QUARTER-FINAL"
                            2 -> "PLAY SEMI-FINAL"
                            else -> "PLAY CUP FINAL"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundCard(
    title: String,
    isCurrent: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF1E293B) else Color(0xFF161F2E)
        ),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = if (isCurrent) Color(0xFFFFD54F) else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun MatchFixtureItem(match: TournamentMatch) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (match.isUserMatch) Color(0xFF0F172A) else Color(0x66000000)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Team 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = match.team1.flagEmoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = match.team1.name,
                    color = if (match.winner?.id == match.team1.id) Color(0xFFFFD54F) else Color.White,
                    fontWeight = if (match.isUserMatch) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            // Score or VS
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF334155)
            ) {
                Text(
                    text = if (match.isCompleted) "${match.score1} - ${match.score2}" else "VS",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Team 2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = match.team2.name,
                    color = if (match.winner?.id == match.team2.id) Color(0xFFFFD54F) else Color.White,
                    fontWeight = if (match.isUserMatch) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = match.team2.flagEmoji, fontSize = 16.sp)
            }
        }
    }
}
