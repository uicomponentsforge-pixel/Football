package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundSynthesizer
import com.example.data.GameRecordsRepository
import com.example.model.AiDifficulty
import com.example.model.DefaultTeams
import com.example.model.GameMode
import com.example.model.MatchConfig
import com.example.model.StadiumType
import com.example.model.Team

@Composable
fun MainMenuScreen(
    onStartQuickMatch: (MatchConfig) -> Unit,
    onStartTournament: (userTeam: Team, difficulty: AiDifficulty) -> Unit,
    onStartPenaltyShootout: (userTeam: Team, compTeam: Team, difficulty: AiDifficulty) -> Unit,
    onStartFreeKick: (userTeam: Team, compTeam: Team) -> Unit
) {
    val context = LocalContext.current
    val recordsRepo = remember { GameRecordsRepository(context) }
    val careerStats = remember { recordsRepo.getCareerStats() }

    var selectedMode by remember { mutableStateOf(GameMode.QUICK_MATCH) }
    var selectedUserTeam by remember { mutableStateOf(DefaultTeams.RED_STRIKERS) }
    var selectedCompTeam by remember { mutableStateOf(DefaultTeams.BLUE_TITANS) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    var selectedDuration by remember { mutableStateOf(90) }
    var selectedStadium by remember { mutableStateOf(StadiumType.EMERALD_ARENA) }
    var isMuted by remember { mutableStateOf(SoundSynthesizer.isSoundMuted()) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("screen_main_menu")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = Color(0xFF2E7D32)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⚽", fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FOOTBALL 2D",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Player vs Computer Arcade",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                SoundSynthesizer.setMuted(isMuted)
                            },
                            modifier = Modifier.testTag("btn_mute_toggle")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.testTag("btn_help")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = "Controls Guide",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Career Quick Stats Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CareerStatItem(label = "Matches", value = "${careerStats.matchesPlayed}")
                        CareerStatItem(label = "Wins", value = "${careerStats.wins}", highlight = true)
                        CareerStatItem(label = "Win Rate", value = "${careerStats.winRate}%")
                        CareerStatItem(label = "Goals", value = "${careerStats.goalsScored}")
                        CareerStatItem(label = "Cups", value = "${careerStats.tournamentsWon} 🏆", highlight = true)
                    }
                }
            }

            // Game Mode Selection Tabs
            item {
                Text(
                    text = "SELECT GAME MODE",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameMode.values().forEach { mode ->
                        val isSel = selectedMode == mode
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = mode }
                                .testTag("mode_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) Color(0xFF2E7D32) else Color(0xFF1E293B)
                            ),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (mode) {
                                        GameMode.QUICK_MATCH -> "⚡"
                                        GameMode.TOURNAMENT -> "🏆"
                                        GameMode.PENALTY_SHOOTOUT -> "🎯"
                                        GameMode.FREE_KICK -> "🚀"
                                    },
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mode.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Team Picker (Player Team vs Computer Team)
            item {
                Text(
                    text = "CHOOSE YOUR TEAM",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(DefaultTeams.ALL_TEAMS) { team ->
                        val isSel = (team.id == selectedUserTeam.id)
                        TeamCard(team = team, isSelected = isSel) {
                            selectedUserTeam = team
                            if (selectedCompTeam.id == team.id) {
                                selectedCompTeam = DefaultTeams.ALL_TEAMS.first { it.id != team.id }
                            }
                        }
                    }
                }
            }

            if (selectedMode != GameMode.TOURNAMENT) {
                item {
                    Text(
                        text = "COMPUTER OPPONENT TEAM",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(DefaultTeams.ALL_TEAMS.filter { it.id != selectedUserTeam.id }) { team ->
                            val isSel = (team.id == selectedCompTeam.id)
                            TeamCard(team = team, isSelected = isSel) {
                                selectedCompTeam = team
                            }
                        }
                    }
                }
            }

            // AI Difficulty Selector
            item {
                Text(
                    text = "AI DIFFICULTY",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiDifficulty.values().forEach { diff ->
                        val isSel = selectedDifficulty == diff
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff.title, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }

            // Match Duration & Stadium (for Quick Match)
            if (selectedMode == GameMode.QUICK_MATCH) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Duration
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "DURATION", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(60, 90, 120).forEach { sec ->
                                    FilterChip(
                                        selected = selectedDuration == sec,
                                        onClick = { selectedDuration = sec },
                                        label = { Text("${sec}s", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0288D1),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // Stadium
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "STADIUM", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(StadiumType.EMERALD_ARENA, StadiumType.CYBER_DOME).forEach { st ->
                                    FilterChip(
                                        selected = selectedStadium == st,
                                        onClick = { selectedStadium = st },
                                        label = { Text(st.title.split(" ").first(), fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0288D1),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Big Action Launch Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        SoundSynthesizer.playWhistle()
                        when (selectedMode) {
                            GameMode.QUICK_MATCH -> {
                                val cfg = MatchConfig(
                                    userTeam = selectedUserTeam,
                                    compTeam = selectedCompTeam,
                                    difficulty = selectedDifficulty,
                                    durationSeconds = selectedDuration,
                                    stadium = selectedStadium,
                                    mode = GameMode.QUICK_MATCH
                                )
                                onStartQuickMatch(cfg)
                            }
                            GameMode.TOURNAMENT -> {
                                onStartTournament(selectedUserTeam, selectedDifficulty)
                            }
                            GameMode.PENALTY_SHOOTOUT -> {
                                onStartPenaltyShootout(selectedUserTeam, selectedCompTeam, selectedDifficulty)
                            }
                            GameMode.FREE_KICK -> {
                                onStartFreeKick(selectedUserTeam, selectedCompTeam)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("btn_start_game"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (selectedMode) {
                            GameMode.QUICK_MATCH -> "KICK OFF MATCH!"
                            GameMode.TOURNAMENT -> "START CHAMPIONSHIP CUP!"
                            GameMode.PENALTY_SHOOTOUT -> "START PENALTY SHOOTOUT!"
                            GameMode.FREE_KICK -> "START FREE KICK DRILL!"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Help / Controls Guide Dialog
        if (showHelpDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable { showHelpDialog = false }
                    .testTag("dialog_help"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📖 HOW TO PLAY",
                            color = Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ControlGuideRow(key = "🕹️ Joystick", desc = "Move active controlled player smoothly.")
                        ControlGuideRow(key = "⚡ Shoot / Super", desc = "Tap to shoot. Hold to charge full power Super Shot!")
                        ControlGuideRow(key = "⚽ Pass", desc = "Passes to nearest open teammate in your direction.")
                        ControlGuideRow(key = "🚀 Chip / Lob", desc = "Lofts ball high over defenders & goalkeeper.")
                        ControlGuideRow(key = "🛑 Slide Tackle", desc = "Slide burst to tackle and steal the ball.")
                        ControlGuideRow(key = "💨 Sprint", desc = "Boosts player running speed with stamina.")

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showHelpDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("GOT IT!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerStatItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = if (highlight) Color(0xFFFFD54F) else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TeamCard(team: Team, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
            .testTag("team_card_${team.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF161F2E)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)) else null
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Kit & flag icon
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = team.primaryColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = team.flagEmoji, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = team.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ATK ${team.attackRating}", color = Color(0xFFFF5252), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("DEF ${team.defenseRating}", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ControlGuideRow(key: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = key, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
        Text(text = desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
    }
}
