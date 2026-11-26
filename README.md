# Grand Line Escape — Java Backend Architecture

A top-down action escape game themed around **One Piece**, engineered with an authoritative Java backend (Jetty HTTP + WebSocket, 30 Hz simulation engine) and a **60 FPS Browser Canvas Client**.

---

## Complete Architecture (`src/` + `web/`)

```text
GrandLine-JavaBackend/
├── src/
│   ├── Main.java               # Entry point: runs boundary tests, starts server & opens browser
│   ├── GameServer.java         # Jetty HTTP server, WebSocket state stream, and REST JSON API
│   ├── GameEngine.java         # 30 Hz simulation engine, world builder, collisions & JSON serializer
│   ├── Player.java             # Monkey D. Luffy class (HP, Energy, Dash, Weapons, String methods)
│   ├── Weapon.java             # 6 weapons with Common -> Legendary rarity upgrades
│   ├── PirateEnemy.java        # Villain pirates & multi-phase Final Boss IMU
│   ├── RarityChest.java        # Weighted Math.random() chests replacing trivia questions for HP/Energy
│   ├── SectorGate.java         # Progression barriers unlocked with Green Log Poses & Gold Keycards
│   ├── WorldObstacle.java      # Solid walls/rocks/pillars with Circle-vs-AABB sliding collision math
│   ├── LootDrop.java           # Physical berry and Log Pose pickups
│   ├── Projectile.java         # Active projectiles with piercing support
│   ├── SkyLaser.java           # IMU boss orbital sky-beams
│   └── BoundaryTestSuite.java  # Automated gameplay boundary and input-validation tests
├── pom.xml                     # Maven build and Jetty WebSocket dependencies
├── web/
│   └── index.html              # 60 FPS renderer, client-predicted movement, audio, level selection and HUD
├── Dockerfile                  # Multi-stage Java image for deployment
├── render.yaml                 # Render Blueprint web-service configuration
├── run.bat                     # One-click Windows compile & launch script
├── run.sh                      # One-click macOS / Linux compile & launch script
└── README.md                   # Architecture & gameplay guide
```

---

## How to Run

Requires Java 21+ and Maven.

### Option A: One-Click Scripts
- **Windows:** Double-click **`run.bat`**.
- **macOS / Linux:** Open Terminal inside the extracted folder and run:
  ```bash
  chmod +x run.sh
  ./run.sh
  ```

### Option B: Manual Terminal Commands
```bash
mvn -DskipTests package
java -jar target/game-server.jar
```
Once started, open **`http://localhost:8080`** in your browser (it opens automatically on most desktops).

### Deploy to Render

Use **New > Blueprint** in Render and connect this repository. Render reads `render.yaml`, builds the Java service from `Dockerfile`, and uses `/health` for health checks. The server binds to Render's `PORT` environment variable; local runs default to port 8080.

The browser runs player, enemy, projectile, and camera movement at display frame rate. It sends movement and position updates over a persistent WebSocket connection; Java keeps collision, combat, damage, and progression checks authoritative, and pushes state snapshots 15 times per second. Discrete actions and game start remain HTTP requests. If the WebSocket disconnects, the browser reconnects and temporarily falls back to HTTP updates. The server advances gameplay at 30 ticks per second while the browser renders at 60 FPS. An original swashbuckling theme and synthesized sound effects run in the browser; shot sounds play only when ammunition is consumed. Use the **Audio** toggle to mute them.

The map has five surface sections, each with twenty physical crates and trees arranged in alternating zigzags; tree trunks block players and projectiles. The Secret Vault approach is kept clear of obstacles, and enemies, chests, and portals are also kept clear of them. The Boss Card Warden in section five drops the Boss Card that unlocks the IMU portal, now located in that same section. The Boss Card stays with you until the run ends, so you can reuse the portal freely. IMU retains boss-specific laser attacks in the Empty Throne realm.

---

## How to Play

1. Choose Easy, Normal, Hard, or Expert and a captain name on the home screen, then select **Start game**. Easy uses weaker enemies; each difficulty has 2, 4, 6, or 8 enemies in each surface section.
2. You start with **0 berries**. Defeated pirates leave berry pickups and have a **50% chance** to drop a Log Pose; move close to collect either item. Berry rewards are about **15% higher** than before.
3. Chests must be opened with **E** when nearby, provide health, energy, and berries, and return after 40 seconds. Chests appear in every surface section and in the Empty Throne realm.
4. Cross the five sections, collecting Log Poses to open the guarded gates. Defeat the Boss Card Warden in section 5 and collect the Boss Card to unlock the IMU portal there. The Boss Card is reusable for the rest of the run. Enter the Gold Key portal in section 2 to fight three stronger, non-respawning guardians in their own realm; each drops one Gold Key. Regular enemies and chests do not drop Gold Keys. Use all three keys to open the Secret Poneglyph Vault and claim Gear 5.
5. Spend berries at the armory and forge. Armory weapons cost **15, 28, 45, and 68 berries**; rarity upgrades cost **23, 45, 80, then 140 berries**. Armory listings show weapon damage, ammo capacity, and energy cost; the currently selected weapon is highlighted.
6. Press **B** to return to base from anywhere, spending one Log Pose. Pirates cannot attack and weapons cannot be fired while you are on the full brown harbor pad.
7. Pirates return at their defeat location after **3 minutes**. Enemy firing rates are 30% lower, and all enemies—including bosses—deal 50% less damage than before. All weapons take **3 seconds** to reload; energy regenerates at **1 point per second**, and each shot costs 1 energy. Enemy health and damage scale with difficulty.
8. Use the portal in section 5 to reach IMU's separate Empty Throne realm. Defeat the boss, collect the Escape Key it drops, return through the portal, then press **E** at the ship to win.

## Controls

- **Move Luffy:** `W` `A` `S` `D` or Arrow Keys
- **Aim & Attack:** Mouse Cursor + `Left Click` *(each shot costs 1 Energy)*
- **Gum-Gum Rocket Dash:** `Space` or `Shift` *(costs 25 Energy; grants brief invulnerability; energy regenerates at 1 per second)*
- **Open Chests / Unlock Gates / Use Armory, Forge & Thousand Sunny:** `E` (stand close; the ship needs IMU's Escape Key)
- **Return to Base:** `B` *(costs 1 Green Log Pose)*
- **Portals:** press `E` at the section 2 portal to enter the Gold Key realm, or at the section 5 portal to enter the Empty Throne realm. The section 5 portal requires the reusable Boss Card from the Boss Card Warden. Each realm has a return portal.
- **Gear 5:** claim the vault reward with `E` to increase player damage by 40%, raise maximum health to 150, and regenerate 2 energy per second. It is an upgrade, not a weapon.
- **Pause / Resume:** `P` *(the pause screen also lets you return home)*
- **Instructions:** `I`
- **Reload Weapon:** `R`
- **Switch Weapons (Slots 1–5):** Number Keys `1`–`5`
