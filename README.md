# Football 2D - Arcade Match

An action-packed HTML5 Canvas 2D arcade football (soccer) game built with modular JavaScript, custom Web Audio synthesis, and responsive CSS overlays.

## Features

- **Multiple Game Modes**:
  - **Quick Match**: Instant 1v1 match against computer AI with configurable match duration and difficulty.
  - **Championship Cup**: Tournament bracket mode featuring Quarter-Finals, Semi-Finals, and Final matches.
  - **Penalty Shootout**: High-stakes penalty shootout competition.
  - **Free Kick Drill**: Target practice for mastering curved and powered shots.
- **AI Difficulty Levels**: Rookie (Easy), Pro (Medium), Legend (Hard), and World Class (Master).
- **Multiple Stadium Environments**: Emerald Arena, Sunset Stadium, Cyber Dome, and Winter Pitch.
- **Dynamic HUD & Visuals**:
  - Top scoreboard with real-time clock and score tracking.
  - Radar mini-map for tactical player positioning.
  - On-screen touch/virtual joystick and action buttons for mobile & desktop play.
  - Animated goal celebration banners and end-of-match statistics summary.
- **Synthesized Web Audio**: Built-in Web Audio API sound generator for whistles, kicks, crowd cheers, referee whistles, and goal celebrations without external audio assets.
- **Career & Match Statistics**: Real-time tracking of played matches, wins, win rate %, goals scored, and trophies won stored in browser `localStorage`.

## Controls

| Action | Keyboard / Mouse | Mobile / Touch |
| --- | --- | --- |
| **Movement** | WASD or Arrow Keys | On-Screen Virtual Joystick |
| **Pass** | `K` Key | PASS Button |
| **Shoot / Super Shot** | Spacebar or `J` Key (Hold to charge power) | SHOOT Button (Hold to charge power) |
| **Chip / Lob** | `L` Key | LOB Button |
| **Slide Tackle** | Shift or `I` Key | SLIDE Button |
| **Sprint** | `E` Key (Uses stamina bar) | Sprint control |
| **Pause Game** | `P` Key or Pause Button | Pause Button |

## Project Structure

```
.
├── index.html              # Main HTML markup and UI overlay containers
├── style.css               # Styling for game overlays, menus, HUD, and responsiveness
└── js/
    ├── Vec2.js             # 2D Vector physics and math utility class
    ├── SoundSynthesizer.js # Web Audio API sound effect and synthesizer manager
    ├── FootballGameEngine.js # Core game loop, physics engine, AI rules, and canvas renderer
    └── app.js              # Application controller, UI event handling, and state management
```

## How to Run

Because this is a pure front-end HTML5 application using standard ES6 modules / scripts, you can run it with any static web server:

Using Python 3:
```bash
python3 -m http.server 8000
```

Then open your browser and navigate to `http://localhost:8000`.

## License

MIT License. Feel free to use, modify, and build upon this game engine.
