/**
 * Main Application JS Controller
 * Handles UI interactions, game loop rendering, tournament progression, and controls.
 */

// Teams Data
const DefaultTeams = {
    RED_STRIKERS: { id: "red_strikers", name: "Red Strikers", shortName: "RED", flagEmoji: "🔴", primaryColor: "#D32F2F", attackRating: 88, defenseRating: 80 },
    BLUE_TITANS: { id: "blue_titans", name: "Blue Titans", shortName: "BLU", flagEmoji: "🔵", primaryColor: "#1976D2", attackRating: 82, defenseRating: 90 },
    GOLD_LIONS: { id: "gold_lions", name: "Golden Lions", shortName: "GLD", flagEmoji: "🦁", primaryColor: "#F57F17", attackRating: 89, defenseRating: 84 },
    GREEN_DRAGONS: { id: "green_dragons", name: "Green Dragons", shortName: "GRN", flagEmoji: "🐉", primaryColor: "#2E7D32", attackRating: 84, defenseRating: 86 },
    CYBER_UNITED: { id: "cyber_united", name: "Cyber United", shortName: "CYB", flagEmoji: "⚡", primaryColor: "#00ACC1", attackRating: 91, defenseRating: 79 },
    ROYAL_PURPLE: { id: "royal_purple", name: "Royal Monarchs", shortName: "RYL", flagEmoji: "👑", primaryColor: "#7B1FA2", attackRating: 86, defenseRating: 88 },
    SOLAR_FLAME: { id: "solar_flame", name: "Solar Phoenix", shortName: "SOL", flagEmoji: "🔥", primaryColor: "#E64A19", attackRating: 93, defenseRating: 76 },
    SHADOW_KNIGHTS: { id: "shadow_knights", name: "Shadow Knights", shortName: "SHD", flagEmoji: "⚔️", primaryColor: "#212121", attackRating: 85, defenseRating: 92 }
};

const ALL_TEAMS = Object.values(DefaultTeams);

const StadiumTypes = {
    EMERALD_ARENA: { title: "Emerald Arena", dark: "#1B5E20", light: "#2E7D32" },
    SUNSET_STADIUM: { title: "Sunset Stadium", dark: "#264653", light: "#2A9D8F" },
    CYBER_DOME: { title: "Cyber Dome", dark: "#0F172A", light: "#1E293B" },
    WINTER_PITCH: { title: "Winter Pitch", dark: "#334155", light: "#475569" }
};

class GameRecordsRepository {
    getCareerStats() {
        const stored = localStorage.getItem('career_stats');
        if (stored) {
            return JSON.parse(stored);
        }
        return { matchesPlayed: 0, wins: 0, goalsScored: 0, tournamentsWon: 0 };
    }

    recordMatchResult(userGoals, compGoals) {
        const stats = this.getCareerStats();
        stats.matchesPlayed++;
        stats.goalsScored += userGoals;
        if (userGoals > compGoals) stats.wins++;
        localStorage.setItem('career_stats', JSON.stringify(stats));
        return stats;
    }

    recordTournamentWin() {
        const stats = this.getCareerStats();
        stats.tournamentsWon++;
        localStorage.setItem('career_stats', JSON.stringify(stats));
    }
}

const recordsRepo = new GameRecordsRepository();

// Global App State
let activeEngine = null;
let animationFrameId = null;
let isPaused = false;
let currentMode = "QUICK_MATCH";
let selectedUserTeam = DefaultTeams.RED_STRIKERS;
let selectedCompTeam = DefaultTeams.BLUE_TITANS;
let selectedDifficulty = AiDifficulty.MEDIUM;
let selectedDuration = 90;
let selectedStadium = StadiumTypes.EMERALD_ARENA;

// Mouse / Cursor Controls State
const mouseState = {
    isDown: false,
    x: 0,
    y: 0,
    active: false
};

// Tournament state
let tournamentRound = 1; // 1: QF, 2: SF, 3: Final, 4: Champion
let qfMatches = [];
let sfMatches = [];
let finalMatch = null;

// Controls Key State
const keyState = {
    up: false, down: false, left: false, right: false,
    sprint: false, shoot: false, pass: false, lob: false, slide: false
};

// Canvas references
const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const radarCanvas = document.getElementById('radarCanvas');
const radarCtx = radarCanvas.getContext('2d');

document.addEventListener('DOMContentLoaded', () => {
    initUI();
    initKeyboardControls();
    initJoystickControls();
    initMouseControls();
    updateCareerStatsUI();
});

function initUI() {
    renderTeamSelectors();

    // Mode Selector Buttons
    document.querySelectorAll('#mode-selector .mode-card').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#mode-selector .mode-card').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentMode = btn.dataset.mode;
            document.getElementById('comp-team-section').style.display = (currentMode === 'TOURNAMENT') ? 'none' : 'block';
            document.getElementById('match-settings-row').style.display = (currentMode === 'QUICK_MATCH') ? 'grid' : 'none';
        });
    });

    // Difficulty Buttons
    document.querySelectorAll('#difficulty-selector .chip-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#difficulty-selector .chip-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            selectedDifficulty = AiDifficulty[btn.dataset.diff];
        });
    });

    // Duration Buttons
    document.querySelectorAll('#duration-selector .chip-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#duration-selector .chip-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            selectedDuration = parseInt(btn.dataset.sec, 10);
        });
    });

    // Stadium Buttons
    document.querySelectorAll('#stadium-selector .chip-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#stadium-selector .chip-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            selectedStadium = StadiumTypes[btn.dataset.stadium];
        });
    });

    // Audio & Help
    document.getElementById('btn-toggle-sound').addEventListener('click', () => {
        const isMuted = !soundSynthesizer.isMuted();
        soundSynthesizer.setMuted(isMuted);
        document.getElementById('sound-icon').textContent = isMuted ? '🔇' : '🔊';
    });

    document.getElementById('btn-open-help').addEventListener('click', () => {
        document.getElementById('dialog-help').classList.remove('hidden');
    });

    document.getElementById('btn-close-help').addEventListener('click', () => {
        document.getElementById('dialog-help').classList.add('hidden');
    });

    // Launch Match
    document.getElementById('btn-start-game').addEventListener('click', () => {
        soundSynthesizer.playWhistle();
        if (currentMode === 'TOURNAMENT') {
            startTournamentMode();
        } else {
            startQuickMatch();
        }
    });

    // Tournament Back
    document.getElementById('btn-tournament-back').addEventListener('click', () => {
        showScreen('screen-main-menu');
    });

    // Play Tournament Match
    document.getElementById('btn-play-tournament-match').addEventListener('click', () => {
        launchCurrentTournamentMatch();
    });

    // Pause Modal Buttons
    document.getElementById('btn-pause-game').addEventListener('click', () => {
        pauseMatch();
    });

    document.getElementById('btn-resume-match').addEventListener('click', () => {
        resumeMatch();
    });

    document.getElementById('btn-restart-match').addEventListener('click', () => {
        document.getElementById('dialog-pause').classList.add('hidden');
        startQuickMatch();
    });

    document.getElementById('btn-exit-to-menu').addEventListener('click', () => {
        exitToMainMenu();
    });

    // Summary Modal Buttons
    document.getElementById('btn-summary-rematch').addEventListener('click', () => {
        document.getElementById('dialog-summary').classList.add('hidden');
        startQuickMatch();
    });

    document.getElementById('btn-summary-main-menu').addEventListener('click', () => {
        exitToMainMenu();
    });

    // On-Screen Action Buttons
    const btnShoot = document.getElementById('btn-shoot');
    btnShoot.addEventListener('pointerdown', () => {
        if (activeEngine) activeEngine.onShootPressed();
    });
    btnShoot.addEventListener('pointerup', () => {
        if (activeEngine) activeEngine.onShootReleased();
    });

    document.getElementById('btn-pass').addEventListener('click', () => {
        if (activeEngine) activeEngine.performPass();
    });

    document.getElementById('btn-lob').addEventListener('click', () => {
        if (activeEngine) activeEngine.performLob();
    });

    document.getElementById('btn-slide').addEventListener('click', () => {
        if (activeEngine) activeEngine.performSlideTackle();
    });
}

function renderTeamSelectors() {
    const userContainer = document.getElementById('user-team-selector');
    const compContainer = document.getElementById('comp-team-selector');
    userContainer.innerHTML = '';
    compContainer.innerHTML = '';

    ALL_TEAMS.forEach(team => {
        const userCard = createTeamCardHTML(team, team.id === selectedUserTeam.id);
        userCard.addEventListener('click', () => {
            selectedUserTeam = team;
            if (selectedCompTeam.id === team.id) {
                selectedCompTeam = ALL_TEAMS.find(t => t.id !== team.id);
            }
            renderTeamSelectors();
        });
        userContainer.appendChild(userCard);

        if (team.id !== selectedUserTeam.id) {
            const compCard = createTeamCardHTML(team, team.id === selectedCompTeam.id);
            compCard.addEventListener('click', () => {
                selectedCompTeam = team;
                renderTeamSelectors();
            });
            compContainer.appendChild(compCard);
        }
    });
}

function createTeamCardHTML(team, isSelected) {
    const card = document.createElement('div');
    card.className = `team-card ${isSelected ? 'active' : ''}`;
    card.innerHTML = `
        <div class="team-avatar" style="background-color: ${team.primaryColor}">
            ${team.flagEmoji}
        </div>
        <span class="team-name">${team.name}</span>
        <div class="team-stats">
            <span class="atk-rating">ATK ${team.attackRating}</span>
            <span class="def-rating">DEF ${team.defenseRating}</span>
        </div>
    `;
    return card;
}

function updateCareerStatsUI() {
    const stats = recordsRepo.getCareerStats();
    document.getElementById('stat-matches').textContent = stats.matchesPlayed;
    document.getElementById('stat-wins').textContent = stats.wins;
    const winRate = stats.matchesPlayed > 0 ? Math.round((stats.wins / stats.matchesPlayed) * 100) : 0;
    document.getElementById('stat-winrate').textContent = `${winRate}%`;
    document.getElementById('stat-goals').textContent = stats.goalsScored;
    document.getElementById('stat-cups').textContent = `${stats.tournamentsWon} 🏆`;
}

function showScreen(screenId) {
    document.querySelectorAll('.screen-layer').forEach(s => s.classList.add('hidden'));
    document.getElementById(screenId).classList.remove('hidden');
}

function showHUD(show) {
    const hudElements = ['hud-scoreboard', 'hud-radar', 'hud-controls'];
    hudElements.forEach(id => {
        if (show) document.getElementById(id).classList.remove('hidden');
        else document.getElementById(id).classList.add('hidden');
    });
}

// Quick Match Setup
function startQuickMatch() {
    const matchCfg = {
        userTeam: selectedUserTeam,
        compTeam: selectedCompTeam,
        difficulty: selectedDifficulty,
        durationSeconds: selectedDuration,
        stadium: selectedStadium,
        mode: currentMode
    };
    launchMatch(matchCfg);
}

function launchMatch(config) {
    showScreen('screen-main-menu'); // Hide menus
    document.getElementById('screen-main-menu').classList.add('hidden');
    document.getElementById('screen-tournament').classList.add('hidden');
    showHUD(true);

    document.getElementById('hud-user-flag').textContent = config.userTeam.flagEmoji;
    document.getElementById('hud-user-name').textContent = config.userTeam.shortName;
    document.getElementById('hud-comp-flag').textContent = config.compTeam.flagEmoji;
    document.getElementById('hud-comp-name').textContent = config.compTeam.shortName;

    if (animationFrameId) cancelAnimationFrame(animationFrameId);

    activeEngine = new FootballGameEngine(
        config,
        (isUserGoal, scorerName) => {
            showGoalBanner(isUserGoal, scorerName);
        },
        (stats) => {
            recordsRepo.recordMatchResult(stats.userGoals, stats.compGoals);
            updateCareerStatsUI();
            showMatchSummary(stats, config);
        }
    );

    isPaused = false;
    let lastTime = performance.now();

    function loop(now) {
        const dt = Math.min(0.033, (now - lastTime) / 1000);
        lastTime = now;

        if (!isPaused && activeEngine) {
            updateKeyboardControls();
            updateMouseControls();
            activeEngine.update(dt);
            renderGame(activeEngine);
            renderRadar(activeEngine);
            updateHUD(activeEngine);
        }

        animationFrameId = requestAnimationFrame(loop);
    }

    animationFrameId = requestAnimationFrame(loop);
}

function pauseMatch() {
    if (!activeEngine) return;
    isPaused = true;
    activeEngine.matchPhase = MatchPhase.PAUSED;
    populateStatsTable('pause-stats-table', activeEngine.stats);
    document.getElementById('dialog-pause').classList.remove('hidden');
}

function resumeMatch() {
    if (!activeEngine) return;
    document.getElementById('dialog-pause').classList.add('hidden');
    isPaused = false;
    activeEngine.matchPhase = MatchPhase.IN_PLAY;
}

function exitToMainMenu() {
    if (animationFrameId) cancelAnimationFrame(animationFrameId);
    activeEngine = null;
    showHUD(false);
    document.getElementById('dialog-pause').classList.add('hidden');
    document.getElementById('dialog-summary').classList.add('hidden');
    showScreen('screen-main-menu');
}

function showGoalBanner(isUserGoal, scorerName) {
    const banner = document.getElementById('goal-banner');
    const title = document.getElementById('banner-title');
    const subtitle = document.getElementById('banner-subtitle');

    title.textContent = activeEngine.celebrationMessage;
    subtitle.textContent = `Scored by ${scorerName}`;

    banner.classList.remove('hidden');
    setTimeout(() => {
        banner.classList.add('hidden');
    }, 2800);
}

function showMatchSummary(stats, config) {
    showHUD(false);
    const userWon = stats.userGoals > stats.compGoals;
    const isDraw = stats.userGoals === stats.compGoals;

    const summaryTitle = document.getElementById('summary-title');
    if (userWon) {
        summaryTitle.textContent = "🏆 VICTORY!";
        summaryTitle.style.color = "#FFD54F";
    } else if (isDraw) {
        summaryTitle.textContent = "🤝 DRAW MATCH";
        summaryTitle.style.color = "#00E5FF";
    } else {
        summaryTitle.textContent = "DEFEAT";
        summaryTitle.style.color = "#FF5252";
    }

    document.getElementById('summary-user-team').textContent = config.userTeam.shortName;
    document.getElementById('summary-score').textContent = `${stats.userGoals} - ${stats.compGoals}`;
    document.getElementById('summary-comp-team').textContent = config.compTeam.shortName;

    populateStatsTable('summary-stats-table', stats);
    document.getElementById('dialog-summary').classList.remove('hidden');
}

function populateStatsTable(tableId, stats) {
    const totalPoss = stats.userPossessionFrames + stats.compPossessionFrames;
    const userPossPct = totalPoss > 0 ? Math.round((stats.userPossessionFrames / totalPoss) * 100) : 50;
    const compPossPct = 100 - userPossPct;

    const container = document.getElementById(tableId);
    container.innerHTML = `
        <div class="stat-row">
            <span class="stat-row-u">${userPossPct}%</span>
            <span class="stat-row-lbl">Possession</span>
            <span class="stat-row-c">${compPossPct}%</span>
        </div>
        <div class="stat-row">
            <span class="stat-row-u">${stats.userShots} (${stats.userShotsOnTarget})</span>
            <span class="stat-row-lbl">Shots (On Target)</span>
            <span class="stat-row-c">${stats.compShots} (${stats.compShotsOnTarget})</span>
        </div>
        <div class="stat-row">
            <span class="stat-row-u">${stats.userTackles}</span>
            <span class="stat-row-lbl">Tackles</span>
            <span class="stat-row-c">${stats.compTackles}</span>
        </div>
        <div class="stat-row">
            <span class="stat-row-u">${stats.userPasses}</span>
            <span class="stat-row-lbl">Passes</span>
            <span class="stat-row-c">${stats.compPasses}</span>
        </div>
    `;
}

function updateHUD(engine) {
    document.getElementById('hud-user-score').textContent = engine.userScore;
    document.getElementById('hud-comp-score').textContent = engine.compScore;

    const totalSec = Math.max(0, Math.floor(engine.timeRemainingSeconds));
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    document.getElementById('hud-clock').textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

    const chargeFill = document.getElementById('shoot-charge-fill');
    chargeFill.style.width = `${Math.round(engine.shootCharge * 100)}%`;
}

// -------------------------------------------------------------
// Canvas Renderer
// -------------------------------------------------------------

function renderGame(engine) {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const stadium = engine.config.stadium || StadiumTypes.EMERALD_ARENA;
    const pW = canvas.width;
    const pH = canvas.height;
    const scaleX = pW / engine.pitchWidth;
    const scaleY = pH / engine.pitchHeight;

    // Grass Mow Stripes
    ctx.fillStyle = stadium.dark;
    ctx.fillRect(0, 0, pW, pH);

    const numStripes = 10;
    const stripeW = pW / numStripes;
    ctx.fillStyle = stadium.light;
    for (let i = 0; i < numStripes; i += 2) {
        ctx.fillRect(i * stripeW, 0, stripeW, pH);
    }

    // Pitch Lines
    ctx.strokeStyle = "rgba(255, 255, 255, 0.85)";
    ctx.lineWidth = 2.5;

    // Halfway
    ctx.beginPath();
    ctx.moveTo(pW / 2, 0);
    ctx.lineTo(pW / 2, pH);
    ctx.stroke();

    // Center circle
    ctx.beginPath();
    ctx.arc(pW / 2, pH / 2, 70 * scaleX, 0, Math.PI * 2);
    ctx.stroke();

    // Penalty Areas
    const penW = 140 * scaleX;
    const penH = 260 * scaleY;
    ctx.strokeRect(0, (pH - penH) / 2, penW, penH);
    ctx.strokeRect(pW - penW, (pH - penH) / 2, penW, penH);

    // Goal Nets
    const gTop = engine.goalTop * scaleY;
    const gH = (engine.goalBottom - engine.goalTop) * scaleY;
    const gD = engine.goalDepth * scaleX;

    ctx.fillStyle = "rgba(255, 255, 255, 0.2)";
    ctx.fillRect(0, gTop, gD, gH);
    ctx.fillRect(pW - gD, gTop, gD, gH);

    // Render Mouse Target Cursor Ring on Canvas
    if (mouseState.active && activeEngine) {
        ctx.strokeStyle = "rgba(255, 213, 79, 0.8)";
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(mouseState.x, mouseState.y, 12, 0, Math.PI * 2);
        ctx.stroke();

        ctx.fillStyle = "rgba(255, 213, 79, 0.6)";
        ctx.beginPath();
        ctx.arc(mouseState.x, mouseState.y, 4, 0, Math.PI * 2);
        ctx.fill();
    }

    // Render Particles
    engine.particles.forEach(p => {
        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(p.pos.x * scaleX, p.pos.y * scaleY, p.size * scaleX, 0, Math.PI * 2);
        ctx.fill();
    });

    // Render Players
    engine.players.forEach(p => {
        const px = p.pos.x * scaleX;
        const py = p.pos.y * scaleY;
        const r = p.radius * scaleX;

        // Shadow
        ctx.fillStyle = "rgba(0,0,0,0.3)";
        ctx.beginPath();
        ctx.ellipse(px, py + r * 0.4, r * 1.1, r * 0.6, 0, 0, Math.PI * 2);
        ctx.fill();

        // Controlled Halo
        if (p.isControlled) {
            ctx.strokeStyle = "#FFD54F";
            ctx.lineWidth = 3;
            ctx.beginPath();
            ctx.arc(px, py, r * 1.5, 0, Math.PI * 2);
            ctx.stroke();
        }

        // Body
        ctx.fillStyle = p.isUserTeam ? engine.config.userTeam.primaryColor : engine.config.compTeam.primaryColor;
        ctx.beginPath();
        ctx.arc(px, py, r, 0, Math.PI * 2);
        ctx.fill();

        // Number
        ctx.fillStyle = "#FFFFFF";
        ctx.font = "bold 12px Inter";
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(p.number, px, py);
    });

    // Render Ball
    const bx = engine.ball.pos.x * scaleX;
    const by = engine.ball.pos.y * scaleY - engine.ball.z * scaleY;
    const br = engine.ball.radius * scaleX;

    // Ball Shadow
    ctx.fillStyle = "rgba(0,0,0,0.35)";
    ctx.beginPath();
    ctx.ellipse(engine.ball.pos.x * scaleX, engine.ball.pos.y * scaleY + 2, br * 1.1, br * 0.6, 0, 0, Math.PI * 2);
    ctx.fill();

    // Ball Body
    ctx.fillStyle = engine.isSuperShotActive ? "#FF1744" : "#FFFFFF";
    ctx.beginPath();
    ctx.arc(bx, by, br, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = "#212121";
    ctx.beginPath();
    ctx.arc(bx, by, br * 0.4, 0, Math.PI * 2);
    ctx.fill();
}

function renderRadar(engine) {
    radarCtx.clearRect(0, 0, radarCanvas.width, radarCanvas.height);
    const scaleX = radarCanvas.width / engine.pitchWidth;
    const scaleY = radarCanvas.height / engine.pitchHeight;

    engine.players.forEach(p => {
        radarCtx.fillStyle = p.isUserTeam ? "#00E5FF" : "#FF1744";
        radarCtx.beginPath();
        radarCtx.arc(p.pos.x * scaleX, p.pos.y * scaleY, p.isControlled ? 3.5 : 2, 0, Math.PI * 2);
        radarCtx.fill();
    });

    radarCtx.fillStyle = "#FFEB3B";
    radarCtx.beginPath();
    radarCtx.arc(engine.ball.pos.x * scaleX, engine.ball.pos.y * scaleY, 2.5, 0, Math.PI * 2);
    radarCtx.fill();
}

// -------------------------------------------------------------
// Tournament Bracket Logic
// -------------------------------------------------------------

function startTournamentMode() {
    tournamentRound = 1;
    const remainingTeams = ALL_TEAMS.filter(t => t.id !== selectedUserTeam.id).sort(() => Math.random() - 0.5).slice(0, 7);

    qfMatches = [
        { round: 1, team1: selectedUserTeam, team2: remainingTeams[0], isUserMatch: true, completed: false },
        { round: 1, team1: remainingTeams[1], team2: remainingTeams[2], isUserMatch: false, completed: false },
        { round: 1, team1: remainingTeams[3], team2: remainingTeams[4], isUserMatch: false, completed: false },
        { round: 1, team1: remainingTeams[5], team2: remainingTeams[6], isUserMatch: false, completed: false }
    ];
    sfMatches = [];
    finalMatch = null;

    renderTournamentBracket();
    showScreen('screen-tournament');
}

function renderTournamentBracket() {
    const qfContainer = document.getElementById('qf-fixtures');
    qfContainer.innerHTML = '';
    qfMatches.forEach(m => qfContainer.appendChild(createFixtureElement(m)));

    const sfContainer = document.getElementById('sf-fixtures');
    sfContainer.innerHTML = '';
    sfMatches.forEach(m => sfContainer.appendChild(createFixtureElement(m)));

    const finalContainer = document.getElementById('final-fixtures');
    finalContainer.innerHTML = '';
    if (finalMatch) finalContainer.appendChild(createFixtureElement(finalMatch));

    const title = document.getElementById('tournament-stage-title');
    title.textContent = tournamentRound === 1 ? "Quarter-Finals (8 Teams)" :
                       tournamentRound === 2 ? "Semi-Finals (4 Teams)" :
                       tournamentRound === 3 ? "CUP FINAL!" : "🏆 CHAMPIONS OF THE CUP!";
}

function createFixtureElement(match) {
    const div = document.createElement('div');
    div.className = 'fixture-item';
    const scoreText = match.completed ? `${match.score1} - ${match.score2}` : 'VS';
    div.innerHTML = `
        <span>${match.team1.flagEmoji} ${match.team1.name}</span>
        <span class="fixture-score">${scoreText}</span>
        <span>${match.team2.name} ${match.team2.flagEmoji}</span>
    `;
    return div;
}

function launchCurrentTournamentMatch() {
    let activeMatch = null;
    if (tournamentRound === 1) activeMatch = qfMatches.find(m => m.isUserMatch);
    else if (tournamentRound === 2) activeMatch = sfMatches.find(m => m.isUserMatch);
    else if (tournamentRound === 3) activeMatch = finalMatch;

    if (!activeMatch) return;

    const opponent = activeMatch.team1.id === selectedUserTeam.id ? activeMatch.team2 : activeMatch.team1;
    const matchCfg = {
        userTeam: selectedUserTeam,
        compTeam: opponent,
        difficulty: tournamentRound === 1 ? selectedDifficulty : (tournamentRound === 2 ? AiDifficulty.HARD : AiDifficulty.MASTER),
        durationSeconds: 60,
        stadium: StadiumTypes.EMERALD_ARENA,
        mode: "TOURNAMENT"
    };

    launchMatch(matchCfg);
}

// -------------------------------------------------------------
// Keyboard, Joystick & Mouse Controls
// -------------------------------------------------------------

function initKeyboardControls() {
    window.addEventListener('keydown', (e) => {
        switch (e.code) {
            case 'ArrowUp': case 'KeyW': keyState.up = true; mouseState.active = false; break;
            case 'ArrowDown': case 'KeyS': keyState.down = true; mouseState.active = false; break;
            case 'ArrowLeft': case 'KeyA': keyState.left = true; mouseState.active = false; break;
            case 'ArrowRight': case 'KeyD': keyState.right = true; mouseState.active = false; break;
            case 'KeyE': case 'ShiftLeft': keyState.sprint = true; break;
            case 'Space': case 'KeyJ':
                if (!keyState.shoot && activeEngine) activeEngine.onShootPressed();
                keyState.shoot = true;
                break;
            case 'KeyK':
                if (!keyState.pass && activeEngine) activeEngine.performPass();
                keyState.pass = true;
                break;
            case 'KeyL':
                if (!keyState.lob && activeEngine) activeEngine.performLob();
                keyState.lob = true;
                break;
            case 'KeyI':
                if (!keyState.slide && activeEngine) activeEngine.performSlideTackle();
                keyState.slide = true;
                break;
        }
    });

    window.addEventListener('keyup', (e) => {
        switch (e.code) {
            case 'ArrowUp': case 'KeyW': keyState.up = false; break;
            case 'ArrowDown': case 'KeyS': keyState.down = false; break;
            case 'ArrowLeft': case 'KeyA': keyState.left = false; break;
            case 'ArrowRight': case 'KeyD': keyState.right = false; break;
            case 'KeyE': case 'ShiftLeft': keyState.sprint = false; break;
            case 'Space': case 'KeyJ':
                if (keyState.shoot && activeEngine) activeEngine.onShootReleased();
                keyState.shoot = false;
                break;
            case 'KeyK': keyState.pass = false; break;
            case 'KeyL': keyState.lob = false; break;
            case 'KeyI': keyState.slide = false; break;
        }
    });
}

function updateKeyboardControls() {
    if (!activeEngine || mouseState.active) return;
    let dx = 0;
    let dy = 0;
    if (keyState.up) dy -= 1;
    if (keyState.down) dy += 1;
    if (keyState.left) dx -= 1;
    if (keyState.right) dx += 1;

    if (dx !== 0 || dy !== 0) {
        activeEngine.joystickDirection = new Vec2(dx, dy);
    }
    activeEngine.sprintHeld = keyState.sprint;
}

function initJoystickControls() {
    const base = document.getElementById('joystick-base');
    const thumb = document.getElementById('joystick-thumb');
    let isDragging = false;
    let baseRect = null;

    base.addEventListener('pointerdown', (e) => {
        isDragging = true;
        mouseState.active = false;
        baseRect = base.getBoundingClientRect();
        updateJoystickPosition(e);
    });

    window.addEventListener('pointermove', (e) => {
        if (isDragging) updateJoystickPosition(e);
    });

    window.addEventListener('pointerup', () => {
        if (isDragging) {
            isDragging = false;
            thumb.style.transform = `translate(-50%, -50%)`;
            if (activeEngine && !mouseState.active) activeEngine.joystickDirection = Vec2.zero();
        }
    });

    function updateJoystickPosition(e) {
        if (!baseRect || !activeEngine) return;
        const centerX = baseRect.left + baseRect.width / 2;
        const centerY = baseRect.top + baseRect.height / 2;
        let dx = e.clientX - centerX;
        let dy = e.clientY - centerY;

        const maxDist = baseRect.width / 2;
        const dist = Math.hypot(dx, dy);
        if (dist > maxDist) {
            dx = (dx / dist) * maxDist;
            dy = (dy / dist) * maxDist;
        }

        thumb.style.transform = `translate(calc(-50% + ${dx}px), calc(-50% + ${dy}px))`;
        activeEngine.joystickDirection = new Vec2(dx / maxDist, dy / maxDist);
    }
}

function initMouseControls() {
    canvas.addEventListener('pointermove', (e) => {
        if (!activeEngine) return;
        const rect = canvas.getBoundingClientRect();
        mouseState.x = e.clientX - rect.left;
        mouseState.y = e.clientY - rect.top;
        if (mouseState.isDown) mouseState.active = true;
    });

    canvas.addEventListener('pointerdown', (e) => {
        if (!activeEngine) return;
        mouseState.isDown = true;
        mouseState.active = true;
        const rect = canvas.getBoundingClientRect();
        mouseState.x = e.clientX - rect.left;
        mouseState.y = e.clientY - rect.top;

        // Start shoot charge or pass on click
        activeEngine.onShootPressed();
    });

    window.addEventListener('pointerup', () => {
        if (mouseState.isDown) {
            mouseState.isDown = false;
            if (activeEngine) activeEngine.onShootReleased();
        }
    });
}

function updateMouseControls() {
    if (!activeEngine || !mouseState.active) return;
    const controlled = activeEngine.getControlledPlayer();
    if (!controlled) return;

    const scaleX = canvas.width / activeEngine.pitchWidth;
    const scaleY = canvas.height / activeEngine.pitchHeight;

    const playerCanvasX = controlled.pos.x * scaleX;
    const playerCanvasY = controlled.pos.y * scaleY;

    const dx = mouseState.x - playerCanvasX;
    const dy = mouseState.y - playerCanvasY;
    const dist = Math.hypot(dx, dy);

    if (dist > 10) {
        activeEngine.joystickDirection = new Vec2(dx / dist, dy / dist);
    } else {
        activeEngine.joystickDirection = Vec2.zero();
    }
}
