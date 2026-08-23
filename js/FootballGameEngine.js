/**
 * Football 2D Game Engine and Physics Logic
 */

const PlayerRole = {
    GOALKEEPER: 'GOALKEEPER',
    DEFENDER: 'DEFENDER',
    MIDFIELDER: 'MIDFIELDER',
    STRIKER: 'STRIKER'
};

const AiDifficulty = {
    EASY: { title: "Rookie", speedMultiplier: 0.78, reactionDelay: 18 },
    MEDIUM: { title: "Pro", speedMultiplier: 0.92, reactionDelay: 10 },
    HARD: { title: "Legend", speedMultiplier: 1.05, reactionDelay: 4 },
    MASTER: { title: "World Class", speedMultiplier: 1.15, reactionDelay: 1 }
};

const MatchPhase = {
    KICKOFF: 'KICKOFF',
    IN_PLAY: 'IN_PLAY',
    GOAL_CELEBRATION: 'GOAL_CELEBRATION',
    FULL_TIME: 'FULL_TIME',
    PAUSED: 'PAUSED'
};

class FootballGameEngine {
    constructor(config, onGoalScored = () => {}, onMatchEnded = () => {}) {
        this.config = config;
        this.onGoalScored = onGoalScored;
        this.onMatchEnded = onMatchEnded;

        // Field virtual coordinate dimensions
        this.pitchWidth = 960;
        this.pitchHeight = 600;
        this.goalTop = 220;
        this.goalBottom = 380;
        this.goalDepth = 40;
        this.goalPostRadius = 7;

        this.matchPhase = MatchPhase.KICKOFF;
        this.timeRemainingSeconds = config.durationSeconds || 90;
        this.userScore = 0;
        this.compScore = 0;

        this.stats = {
            userGoals: 0,
            compGoals: 0,
            userShots: 0,
            compShots: 0,
            userShotsOnTarget: 0,
            compShotsOnTarget: 0,
            userPasses: 0,
            compPasses: 0,
            userTackles: 0,
            compTackles: 0,
            userPossessionFrames: 0,
            compPossessionFrames: 0
        };

        this.players = [];
        this.ball = {
            pos: new Vec2(this.pitchWidth / 2, this.pitchHeight / 2),
            vel: Vec2.zero(),
            z: 0,
            vz: 0,
            radius: 9,
            lastKickedByTeam: null,
            lastKickerId: null,
            trail: []
        };
        this.particles = [];

        this.celebrationTimer = 0;
        this.celebrationMessage = "";
        this.lastScorer = null;
        this.isSuperShotActive = false;
        this.superShotTimer = 0;
        this.userTeamKickoff = true;

        this.joystickDirection = Vec2.zero();
        this.sprintHeld = false;
        this.shootCharge = 0;
        this.isChargingShoot = false;

        this.aiDecisionCounter = 0;
        this.lastPassCooldownComp = 0;

        this.setupTeams();
        this.resetPositions(true);
    }

    setupTeams() {
        this.players = [];
        const uTeam = this.config.userTeam;
        const cTeam = this.config.compTeam;

        // User Players (5v5)
        this.players.push({ id: 1, teamId: uTeam.id, isUserTeam: true, role: PlayerRole.GOALKEEPER, number: 1, name: "GK Keeper", pos: new Vec2(70, this.pitchHeight / 2) });
        this.players.push({ id: 2, teamId: uTeam.id, isUserTeam: true, role: PlayerRole.DEFENDER, number: 3, name: "DF Miller", pos: new Vec2(220, 180) });
        this.players.push({ id: 3, teamId: uTeam.id, isUserTeam: true, role: PlayerRole.DEFENDER, number: 4, name: "DF Davis", pos: new Vec2(220, 420) });
        this.players.push({ id: 4, teamId: uTeam.id, isUserTeam: true, role: PlayerRole.MIDFIELDER, number: 8, name: "MF Silva", pos: new Vec2(350, this.pitchHeight / 2) });
        this.players.push({ id: 5, teamId: uTeam.id, isUserTeam: true, role: PlayerRole.STRIKER, number: 10, name: "FW Leo", pos: new Vec2(440, this.pitchHeight / 2) });

        // Comp Players (5v5)
        this.players.push({ id: 6, teamId: cTeam.id, isUserTeam: false, role: PlayerRole.GOALKEEPER, number: 1, name: "GK Khan", pos: new Vec2(this.pitchWidth - 70, this.pitchHeight / 2) });
        this.players.push({ id: 7, teamId: cTeam.id, isUserTeam: false, role: PlayerRole.DEFENDER, number: 2, name: "DF Ramos", pos: new Vec2(this.pitchWidth - 220, 180) });
        this.players.push({ id: 8, teamId: cTeam.id, isUserTeam: false, role: PlayerRole.DEFENDER, number: 5, name: "DF Van Dijk", pos: new Vec2(this.pitchWidth - 220, 420) });
        this.players.push({ id: 9, teamId: cTeam.id, isUserTeam: false, role: PlayerRole.MIDFIELDER, number: 7, name: "MF Luka", pos: new Vec2(this.pitchWidth - 350, this.pitchHeight / 2) });
        this.players.push({ id: 10, teamId: cTeam.id, isUserTeam: false, role: PlayerRole.STRIKER, number: 9, name: "FW Kane", pos: new Vec2(this.pitchWidth - 440, this.pitchHeight / 2) });

        this.players.forEach(p => {
            p.vel = Vec2.zero();
            p.targetPos = new Vec2(p.pos.x, p.pos.y);
            p.baseFormationPos = new Vec2(p.pos.x, p.pos.y);
            p.facingAngle = p.isUserTeam ? 0 : Math.PI;
            p.baseSpeed = 4.2;
            p.radius = 16;
            p.kickCooldown = 0;
            p.slideCooldown = 0;
            p.slideTimer = 0;
            p.isStunned = 0;
            p.isControlled = false;
            p.stamina = 100;
        });

        const striker = this.players.find(p => p.isUserTeam && p.role === PlayerRole.STRIKER);
        this.selectControlledPlayer(striker || this.players[0]);
    }

    resetPositions(userKickoff) {
        this.userTeamKickoff = userKickoff;
        this.ball.pos = new Vec2(this.pitchWidth / 2, this.pitchHeight / 2);
        this.ball.vel = Vec2.zero();
        this.ball.z = 0;
        this.ball.vz = 0;
        this.ball.trail = [];
        this.isSuperShotActive = false;

        this.players.forEach(p => {
            p.vel = Vec2.zero();
            p.pos = new Vec2(p.baseFormationPos.x, p.baseFormationPos.y);
            p.targetPos = new Vec2(p.baseFormationPos.x, p.baseFormationPos.y);
            p.isStunned = 0;
            p.slideTimer = 0;
            p.stamina = 100;
            p.facingAngle = p.isUserTeam ? 0 : Math.PI;
        });

        if (userKickoff) {
            const striker = this.players.find(p => p.isUserTeam && p.role === PlayerRole.STRIKER);
            if (striker) striker.pos = new Vec2(this.pitchWidth / 2 - 22, this.pitchHeight / 2);
            this.selectControlledPlayer(striker);
        } else {
            const compStriker = this.players.find(p => !p.isUserTeam && p.role === PlayerRole.STRIKER);
            if (compStriker) compStriker.pos = new Vec2(this.pitchWidth / 2 + 22, this.pitchHeight / 2);
            const userMid = this.players.find(p => p.isUserTeam && p.role === PlayerRole.MIDFIELDER);
            this.selectControlledPlayer(userMid);
        }

        this.matchPhase = MatchPhase.KICKOFF;
    }

    selectControlledPlayer(player) {
        this.players.filter(p => p.isUserTeam).forEach(p => p.isControlled = false);
        if (player) player.isControlled = true;
    }

    getControlledPlayer() {
        return this.players.find(p => p.isUserTeam && p.isControlled);
    }

    update(dt = 1 / 60) {
        if (this.matchPhase === MatchPhase.PAUSED) return;

        if (this.matchPhase === MatchPhase.GOAL_CELEBRATION) {
            this.celebrationTimer -= dt;
            this.updateParticles(dt);
            if (this.celebrationTimer <= 0) {
                if (this.timeRemainingSeconds <= 0) {
                    this.endMatch();
                } else {
                    this.resetPositions(!this.userTeamKickoff);
                    soundSynthesizer.playWhistle();
                }
            }
            return;
        }

        if (this.matchPhase === MatchPhase.FULL_TIME) {
            this.updateParticles(dt);
            return;
        }

        // Clock
        this.timeRemainingSeconds -= dt;
        if (this.timeRemainingSeconds <= 0) {
            this.timeRemainingSeconds = 0;
            this.endMatch();
            return;
        }

        // Shoot charging
        if (this.isChargingShoot) {
            this.shootCharge = Math.min(1.0, this.shootCharge + dt * 1.6);
        }

        // Possession Tracking
        let minUserDist = 999;
        let minCompDist = 999;
        this.players.forEach(p => {
            const d = p.pos.distanceTo(this.ball.pos);
            if (p.isUserTeam && d < minUserDist) minUserDist = d;
            if (!p.isUserTeam && d < minCompDist) minCompDist = d;
        });

        if (minUserDist < 50 && minUserDist < minCompDist) this.stats.userPossessionFrames++;
        else if (minCompDist < 50 && minCompDist < minUserDist) this.stats.compPossessionFrames++;

        // Auto Switch closest player
        const closestUser = this.players
            .filter(p => p.isUserTeam && p.role !== PlayerRole.GOALKEEPER && p.isStunned === 0)
            .reduce((minP, p) => (!minP || p.pos.distanceTo(this.ball.pos) < minP.pos.distanceTo(this.ball.pos) ? p : minP), null);

        const currentCtrl = this.getControlledPlayer();
        if (closestUser && currentCtrl && closestUser.id !== currentCtrl.id) {
            const dCurrent = currentCtrl.pos.distanceTo(this.ball.pos);
            const dClosest = closestUser.pos.distanceTo(this.ball.pos);
            if (dClosest < dCurrent - 75) {
                this.selectControlledPlayer(closestUser);
            }
        }

        // Movement updates
        this.updateUserPlayer(dt);
        this.updateTeammateAI(dt);
        this.updateComputerAI(dt);

        this.players.forEach(p => this.updatePlayerPhysics(p, dt));
        this.updateBallPhysics(dt);
        this.updateParticles(dt);
        this.updateBallTrail();
        this.checkGoalConditions();

        if (this.isSuperShotActive) {
            this.superShotTimer -= dt;
            if (this.superShotTimer <= 0 || this.ball.vel.length() < 8) {
                this.isSuperShotActive = false;
            }
        }
    }

    updateUserPlayer(dt) {
        const player = this.getControlledPlayer();
        if (!player || player.isStunned > 0) return;

        if (this.joystickDirection.length() > 0.05) {
            const normDir = this.joystickDirection.normalized();
            const speedMult = (this.sprintHeld && player.stamina > 10) ? 1.45 : 1.0;
            if (this.sprintHeld && player.stamina > 10) {
                player.stamina = Math.max(0, player.stamina - dt * 18);
            } else {
                player.stamina = Math.min(100, player.stamina + dt * 8);
            }
            const targetSpeed = player.baseSpeed * speedMult;
            player.vel = new Vec2(
                player.vel.x * 0.75 + normDir.x * targetSpeed * 0.25,
                player.vel.y * 0.75 + normDir.y * targetSpeed * 0.25
            );
            player.facingAngle = normDir.angle();

            if (this.matchPhase === MatchPhase.KICKOFF) {
                this.matchPhase = MatchPhase.IN_PLAY;
            }
        } else {
            player.vel = player.vel.times(0.8);
            player.stamina = Math.min(100, player.stamina + dt * 12);
        }
    }

    updateTeammateAI(dt) {
        const userPlayers = this.players.filter(p => p.isUserTeam && !p.isControlled);
        const controlled = this.getControlledPlayer();

        userPlayers.forEach(p => {
            if (p.isStunned > 0) return;

            if (p.role === PlayerRole.GOALKEEPER) {
                const targetY = Math.max(this.goalTop + 15, Math.min(this.goalBottom - 15, this.ball.pos.y));
                const targetX = 65 + (this.ball.pos.x / this.pitchWidth) * 35;
                this.movePlayerTowards(p, new Vec2(targetX, targetY), p.baseSpeed * 0.9);
                p.facingAngle = this.ball.pos.minus(p.pos).angle();
            } else if (p.role === PlayerRole.DEFENDER) {
                const target = new Vec2(p.baseFormationPos.x + (this.ball.pos.x - this.pitchWidth / 2) * 0.2, p.baseFormationPos.y);
                this.movePlayerTowards(p, target, p.baseSpeed * 0.85);
                p.facingAngle = this.ball.pos.minus(p.pos).angle();
            } else {
                const target = controlled && this.ball.pos.x > this.pitchWidth * 0.4 ?
                    new Vec2(p.baseFormationPos.x + 80, p.baseFormationPos.y + (p.pos.y < controlled.pos.y ? -40 : 40)) :
                    p.baseFormationPos.plus(new Vec2((this.ball.pos.x - this.pitchWidth / 2) * 0.3, (this.ball.pos.y - this.pitchHeight / 2) * 0.2));
                this.movePlayerTowards(p, target, p.baseSpeed * 0.9);
                p.facingAngle = this.ball.pos.minus(p.pos).angle();
            }
        });
    }

    updateComputerAI(dt) {
        const compPlayers = this.players.filter(p => !p.isUserTeam);
        const diff = this.config.difficulty || AiDifficulty.MEDIUM;
        const speedMult = diff.speedMultiplier || 0.92;
        this.aiDecisionCounter++;

        if (this.lastPassCooldownComp > 0) this.lastPassCooldownComp--;

        const compClosest = compPlayers
            .filter(p => p.role !== PlayerRole.GOALKEEPER && p.isStunned === 0)
            .reduce((minP, p) => (!minP || p.pos.distanceTo(this.ball.pos) < minP.pos.distanceTo(this.ball.pos) ? p : minP), null);

        compPlayers.forEach(p => {
            if (p.isStunned > 0) return;

            if (p.role === PlayerRole.GOALKEEPER) {
                const targetY = Math.max(this.goalTop + 15, Math.min(this.goalBottom - 15, this.ball.pos.y));
                const targetX = (this.pitchWidth - 65) - ((this.pitchWidth - this.ball.pos.x) / this.pitchWidth) * 35;
                this.movePlayerTowards(p, new Vec2(targetX, targetY), p.baseSpeed * 0.95 * speedMult);
                p.facingAngle = this.ball.pos.minus(p.pos).angle();

                if (p.pos.distanceTo(this.ball.pos) < 32 && this.ball.z < 25) {
                    this.aiClearBall(p);
                }
            } else {
                const isChaser = (compClosest && p.id === compClosest.id);
                if (isChaser) {
                    this.movePlayerTowards(p, this.ball.pos, p.baseSpeed * speedMult);
                    p.facingAngle = this.ball.pos.minus(p.pos).angle();

                    const distToBall = p.pos.distanceTo(this.ball.pos);
                    if (distToBall < 30 && this.ball.z < 25) {
                        if (this.matchPhase === MatchPhase.KICKOFF) this.matchPhase = MatchPhase.IN_PLAY;

                        const distToGoal = p.pos.distanceTo(new Vec2(0, this.pitchHeight / 2));
                        if (distToGoal < 340 && p.pos.x < this.pitchWidth * 0.45) {
                            if (Math.random() < 0.08) this.aiShootAtGoal(p);
                        } else if (this.lastPassCooldownComp <= 0 && Math.random() < 0.04) {
                            this.aiPassToTeammate(p);
                        }
                    } else if (distToBall < 55 && distToBall > 30 && p.slideCooldown <= 0) {
                        const userWithBall = this.players.find(u => u.isUserTeam && u.pos.distanceTo(this.ball.pos) < 32);
                        if (userWithBall && Math.random() < 0.03) {
                            this.executePlayerSlideTackle(p);
                        }
                    }
                } else {
                    const target = p.baseFormationPos.plus(new Vec2((this.ball.pos.x - this.pitchWidth / 2) * 0.3, (this.ball.pos.y - this.pitchHeight / 2) * 0.2));
                    this.movePlayerTowards(p, target, p.baseSpeed * 0.85 * speedMult);
                    p.facingAngle = this.ball.pos.minus(p.pos).angle();
                }
            }
        });
    }

    aiShootAtGoal(p) {
        const targetY = Math.random() < 0.5 ? this.goalTop + 25 : this.goalBottom - 25;
        const goalTarget = new Vec2(0, targetY);
        const dir = goalTarget.minus(p.pos).normalized();
        const power = 13 + Math.random() * 4;
        this.ball.vel = dir.times(power);
        this.ball.vz = 2.5 + Math.random() * 2;
        this.ball.lastKickedByTeam = p.teamId;
        this.ball.lastKickerId = p.id;
        p.kickCooldown = 15;
        this.stats.compShots++;
        this.stats.compShotsOnTarget++;
        soundSynthesizer.playKick(0.85);
        this.spawnKickParticles(p.pos, dir);
    }

    aiPassToTeammate(p) {
        const teammates = this.players.filter(tm => !tm.isUserTeam && tm.id !== p.id && tm.role !== PlayerRole.GOALKEEPER);
        if (teammates.length === 0) return;
        const best = teammates.reduce((minP, tm) => (!minP || tm.pos.x < minP.pos.x ? tm : minP), null);
        const dir = best.pos.minus(p.pos).normalized();
        const dist = p.pos.distanceTo(best.pos);
        const speed = Math.max(8, Math.min(15, dist * 0.045));
        this.ball.vel = dir.times(speed);
        this.ball.vz = 1.2;
        this.ball.lastKickedByTeam = p.teamId;
        this.ball.lastKickerId = p.id;
        p.kickCooldown = 18;
        this.lastPassCooldownComp = 45;
        this.stats.compPasses++;
        soundSynthesizer.playKick(0.6);
    }

    aiClearBall(p) {
        const target = new Vec2(this.pitchWidth / 2, Math.random() < 0.5 ? 100 : this.pitchHeight - 100);
        const dir = target.minus(p.pos).normalized();
        this.ball.vel = dir.times(16);
        this.ball.vz = 4.5;
        this.ball.lastKickedByTeam = p.teamId;
        this.ball.lastKickerId = p.id;
        p.kickCooldown = 20;
        soundSynthesizer.playKick(0.9);
    }

    movePlayerTowards(p, target, speed) {
        const diff = target.minus(p.pos);
        if (diff.length() > 5) {
            const dir = diff.normalized();
            p.vel = new Vec2(
                p.vel.x * 0.8 + dir.x * speed * 0.2,
                p.vel.y * 0.8 + dir.y * speed * 0.2
            );
        } else {
            p.vel = p.vel.times(0.7);
        }
    }

    updatePlayerPhysics(p, dt) {
        if (p.isStunned > 0) {
            p.isStunned--;
            p.vel = p.vel.times(0.85);
            p.pos = p.pos.plus(p.vel);
            return;
        }

        if (p.slideTimer > 0) {
            p.slideTimer--;
            p.pos = p.pos.plus(p.vel);
            this.spawnSlideParticles(p.pos);

            const opponents = this.players.filter(opp => opp.isUserTeam !== p.isUserTeam && opp.isStunned === 0);
            opponents.forEach(opp => {
                if (opp.pos.distanceTo(p.pos) < p.radius + opp.radius + 12) {
                    opp.isStunned = 50;
                    opp.vel = p.vel.times(0.5);
                    soundSynthesizer.playTackle();
                    if (p.isUserTeam) this.stats.userTackles++; else this.stats.compTackles++;

                    if (this.ball.pos.distanceTo(opp.pos) < 30) {
                        this.ball.vel = p.vel.times(0.8).plus(new Vec2(Math.random() * 4 - 2, Math.random() * 4 - 2));
                        this.ball.lastKickedByTeam = p.teamId;
                        this.ball.lastKickerId = p.id;
                    }
                }
            });
        } else {
            p.pos = p.pos.plus(p.vel);
        }

        if (p.slideCooldown > 0) p.slideCooldown--;
        if (p.kickCooldown > 0) p.kickCooldown--;

        p.pos.x = Math.max(25, Math.min(this.pitchWidth - 25, p.pos.x));
        p.pos.y = Math.max(25, Math.min(this.pitchHeight - 25, p.pos.y));
    }

    updateBallPhysics(dt) {
        this.ball.pos = this.ball.pos.plus(this.ball.vel);
        const friction = this.ball.z <= 0.1 ? 0.982 : 0.992;
        this.ball.vel = this.ball.vel.times(friction);

        if (this.ball.z > 0 || this.ball.vz !== 0) {
            this.ball.z += this.ball.vz;
            this.ball.vz -= 0.38;
            if (this.ball.z <= 0) {
                this.ball.z = 0;
                if (this.ball.vz < -1.5) {
                    this.ball.vz = -this.ball.vz * 0.6;
                    this.ball.vel = this.ball.vel.times(0.92);
                } else {
                    this.ball.vz = 0;
                }
            }
        }

        // Top / Bottom bounds
        if (this.ball.pos.y < this.ball.radius) {
            this.ball.pos.y = this.ball.radius;
            this.ball.vel = new Vec2(this.ball.vel.x * 0.85, -this.ball.vel.y * 0.75);
        } else if (this.ball.pos.y > this.pitchHeight - this.ball.radius) {
            this.ball.pos.y = this.pitchHeight - this.ball.radius;
            this.ball.vel = new Vec2(this.ball.vel.x * 0.85, -this.ball.vel.y * 0.75);
        }

        // Left / Right bounds
        const inGoalY = this.ball.pos.y >= (this.goalTop - 5) && this.ball.pos.y <= (this.goalBottom + 5);
        if (!inGoalY) {
            if (this.ball.pos.x < this.ball.radius) {
                this.ball.pos.x = this.ball.radius;
                this.ball.vel = new Vec2(-this.ball.vel.x * 0.75, this.ball.vel.y * 0.85);
            } else if (this.ball.pos.x > this.pitchWidth - this.ball.radius) {
                this.ball.pos.x = this.pitchWidth - this.ball.radius;
                this.ball.vel = new Vec2(-this.ball.vel.x * 0.75, this.ball.vel.y * 0.85);
            }
        }

        // Player Dribble Touch
        this.players.forEach(p => {
            if (p.isStunned > 0 || p.kickCooldown > 0) return;
            const dist = p.pos.distanceTo(this.ball.pos);
            const touchRadius = p.radius + this.ball.radius + 4;
            if (dist < touchRadius && this.ball.z < 22) {
                const diff = this.ball.pos.minus(p.pos);
                const pushDir = diff.length() > 0.01 ? diff.normalized() : Vec2.fromAngle(p.facingAngle);
                const dribbleSpeed = Math.max(3, p.vel.length()) + 1.2;
                this.ball.vel = pushDir.times(dribbleSpeed).plus(p.vel.times(0.4));
                this.ball.lastKickedByTeam = p.teamId;
                this.ball.lastKickerId = p.id;
            }
        });
    }

    checkGoalConditions() {
        if (this.matchPhase !== MatchPhase.IN_PLAY && this.matchPhase !== MatchPhase.KICKOFF) return;

        // Post hits
        const posts = [
            new Vec2(0, this.goalTop), new Vec2(0, this.goalBottom),
            new Vec2(this.pitchWidth, this.goalTop), new Vec2(this.pitchWidth, this.goalBottom)
        ];

        posts.forEach(post => {
            if (this.ball.pos.distanceTo(post) < this.ball.radius + this.goalPostRadius) {
                const normal = this.ball.pos.minus(post).normalized();
                this.ball.vel = normal.times(Math.max(6, this.ball.vel.length() * 0.8));
                this.ball.vz = 2.5;
                soundSynthesizer.playPostHit();
                this.spawnPostHitParticles(post);
            }
        });

        const insideGoalMouthY = this.ball.pos.y >= (this.goalTop + 5) && this.ball.pos.y <= (this.goalBottom - 5);

        // User goal (Right net)
        if (this.ball.pos.x >= this.pitchWidth + 10 && insideGoalMouthY) {
            this.userScore++;
            this.stats.userGoals++;
            this.triggerGoalCelebration(true);
        }
        // Comp goal (Left net)
        else if (this.ball.pos.x <= -10 && insideGoalMouthY) {
            this.compScore++;
            this.stats.compGoals++;
            this.triggerGoalCelebration(false);
        }
    }

    triggerGoalCelebration(isUserGoal) {
        this.matchPhase = MatchPhase.GOAL_CELEBRATION;
        this.celebrationTimer = 3.2;
        const scorer = this.players.find(p => p.id === this.ball.lastKickerId);
        this.lastScorer = scorer;

        this.celebrationMessage = isUserGoal ?
            (this.isSuperShotActive ? "⚡ ROCKET GOAL! ⚡" : "🎉 GOOOAL!! 🎉") :
            "⚽ OPPONENT SCORED!";

        soundSynthesizer.playGoalCheer();
        this.spawnGoalCelebrationConfetti(isUserGoal ? new Vec2(this.pitchWidth, this.pitchHeight / 2) : new Vec2(0, this.pitchHeight / 2));
        this.onGoalScored(isUserGoal, scorer ? scorer.name : (isUserGoal ? this.config.userTeam.name : this.config.compTeam.name));
    }

    endMatch() {
        this.matchPhase = MatchPhase.FULL_TIME;
        soundSynthesizer.playWhistle();
        this.onMatchEnded(this.stats);
    }

    // Controls Action Methods
    onShootPressed() {
        if (this.matchPhase !== MatchPhase.IN_PLAY && this.matchPhase !== MatchPhase.KICKOFF) return;
        this.isChargingShoot = true;
        this.shootCharge = 0;
    }

    onShootReleased() {
        if (!this.isChargingShoot) return;
        this.isChargingShoot = false;
        const player = this.getControlledPlayer();
        if (!player || player.isStunned > 0) return;

        const distToBall = player.pos.distanceTo(this.ball.pos);
        if (distToBall > player.radius + this.ball.radius + 28 || this.ball.z > 28) return;

        const power = this.shootCharge;
        this.shootCharge = 0;

        const goalY = this.joystickDirection.y !== 0 ?
            Math.max(this.goalTop + 15, Math.min(this.goalBottom - 15, this.pitchHeight / 2 + this.joystickDirection.y * 80)) :
            (Math.random() < 0.5 ? this.goalTop + 25 : this.goalBottom - 25);

        const target = new Vec2(this.pitchWidth, goalY);
        const dir = target.minus(player.pos).normalized();

        const isSuper = power >= 0.88;
        this.isSuperShotActive = isSuper;
        if (isSuper) this.superShotTimer = 2.0;

        const shotSpeed = isSuper ? 22 : 13 + power * 6.5;
        this.ball.vel = dir.times(shotSpeed);
        this.ball.vz = 1.5 + power * 3;
        this.ball.lastKickedByTeam = player.teamId;
        this.ball.lastKickerId = player.id;
        player.kickCooldown = 15;

        this.stats.userShots++;
        this.stats.userShotsOnTarget++;
        soundSynthesizer.playKick(isSuper ? 1.0 : 0.8);
        this.spawnKickParticles(player.pos, dir);

        if (this.matchPhase === MatchPhase.KICKOFF) this.matchPhase = MatchPhase.IN_PLAY;
    }

    performPass() {
        const player = this.getControlledPlayer();
        if (!player || player.isStunned > 0) return;
        if (player.pos.distanceTo(this.ball.pos) > player.radius + this.ball.radius + 32 || this.ball.z > 28) return;

        const teammates = this.players.filter(tm => tm.isUserTeam && tm.id !== player.id && tm.role !== PlayerRole.GOALKEEPER);
        if (teammates.length === 0) return;

        const facingVec = Vec2.fromAngle(player.facingAngle);
        const bestTeammate = teammates.reduce((best, tm) => {
            const toTm = tm.pos.minus(player.pos).normalized();
            const dot = facingVec.x * toTm.x + facingVec.y * toTm.y;
            const score = dot * 2.0 - (player.pos.distanceTo(tm.pos) * 0.002);
            return (!best || score > best.score) ? { tm, score } : best;
        }, null).tm;

        const dir = bestTeammate.pos.minus(player.pos).normalized();
        const dist = player.pos.distanceTo(bestTeammate.pos);
        const passSpeed = Math.max(10, Math.min(17, dist * 0.052));

        this.ball.vel = dir.times(passSpeed);
        this.ball.vz = 0.8;
        this.ball.lastKickedByTeam = player.teamId;
        this.ball.lastKickerId = player.id;
        player.kickCooldown = 14;
        this.stats.userPasses++;

        soundSynthesizer.playKick(0.65);
        this.selectControlledPlayer(bestTeammate);
        if (this.matchPhase === MatchPhase.KICKOFF) this.matchPhase = MatchPhase.IN_PLAY;
    }

    performLob() {
        const player = this.getControlledPlayer();
        if (!player || player.isStunned > 0) return;
        if (player.pos.distanceTo(this.ball.pos) > player.radius + this.ball.radius + 30) return;

        const dir = Vec2.fromAngle(player.facingAngle);
        this.ball.vel = dir.times(12.5);
        this.ball.vz = 5.5;
        this.ball.lastKickedByTeam = player.teamId;
        this.ball.lastKickerId = player.id;
        player.kickCooldown = 18;

        soundSynthesizer.playKick(0.7);
        if (this.matchPhase === MatchPhase.KICKOFF) this.matchPhase = MatchPhase.IN_PLAY;
    }

    executePlayerSlideTackle(player) {
        if (player.isStunned > 0 || player.slideCooldown > 0) return;
        player.slideTimer = 18;
        player.slideCooldown = 60;
        const dir = Vec2.fromAngle(player.facingAngle);
        player.vel = dir.times(player.baseSpeed * 2.4);
        soundSynthesizer.playTackle();
    }

    performSlideTackle() {
        const player = this.getControlledPlayer();
        if (!player || player.isStunned > 0 || player.slideCooldown > 0) return;

        player.slideTimer = 18;
        player.slideCooldown = 60;
        const dir = this.joystickDirection.length() > 0.1 ? this.joystickDirection.normalized() : Vec2.fromAngle(player.facingAngle);
        player.vel = dir.times(player.baseSpeed * 2.4);
        soundSynthesizer.playTackle();
    }

    // Particle & VFX Helpers
    updateParticles(dt) {
        for (let i = this.particles.length - 1; i >= 0; i--) {
            const p = this.particles[i];
            p.pos = p.pos.plus(p.vel);
            p.vel = p.vel.times(0.94);
            p.life -= dt;
            if (p.life <= 0) this.particles.splice(i, 1);
        }
    }

    updateBallTrail() {
        if (this.ball.vel.length() > 4) {
            this.ball.trail.unshift(new Vec2(this.ball.pos.x, this.ball.pos.y));
            if (this.ball.trail.length > 8) this.ball.trail.pop();
        } else if (this.ball.trail.length > 0) {
            this.ball.trail.pop();
        }
    }

    spawnKickParticles(origin, dir) {
        for (let i = 0; i < 8; i++) {
            const speed = Math.random() * 4 + 2;
            const angle = dir.angle() + (Math.random() - 0.5) * 1.2;
            this.particles.push({
                pos: origin.plus(dir.times(15)),
                vel: Vec2.fromAngle(angle, speed),
                color: "#FFD54F",
                size: Math.random() * 4 + 3,
                life: 0.3,
                maxLife: 0.3
            });
        }
    }

    spawnSlideParticles(pos) {
        for (let i = 0; i < 3; i++) {
            this.particles.push({
                pos: pos.plus(new Vec2(Math.random() * 16 - 8, Math.random() * 16 - 8)),
                vel: new Vec2(Math.random() * 3 - 1.5, Math.random() * 3 - 1.5),
                color: "rgba(255, 255, 255, 0.5)",
                size: Math.random() * 5 + 3,
                life: 0.25,
                maxLife: 0.25
            });
        }
    }

    spawnPostHitParticles(post) {
        for (let i = 0; i < 12; i++) {
            const angle = Math.random() * 2 * Math.PI;
            this.particles.push({
                pos: new Vec2(post.x, post.y),
                vel: Vec2.fromAngle(angle, Math.random() * 5 + 2),
                color: "#FFEB3B",
                size: Math.random() * 4 + 2,
                life: 0.4,
                maxLife: 0.4
            });
        }
    }

    spawnGoalCelebrationConfetti(goalPos) {
        const colors = ["#FFD54F", "#FF1744", "#00E5FF", "#76FF03", "#FF4081", "#FFFFFF"];
        for (let i = 0; i < 60; i++) {
            const angle = Math.random() * 2 * Math.PI;
            const speed = Math.random() * 12 + 4;
            this.particles.push({
                pos: goalPos.plus(new Vec2(Math.random() * 40 - 20, Math.random() * 100 - 50)),
                vel: Vec2.fromAngle(angle, speed),
                color: colors[Math.floor(Math.random() * colors.length)],
                size: Math.random() * 7 + 4,
                life: Math.random() * 2.0 + 1.0,
                maxLife: 2.5
            });
        }
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = { FootballGameEngine, PlayerRole, AiDifficulty, MatchPhase };
}
