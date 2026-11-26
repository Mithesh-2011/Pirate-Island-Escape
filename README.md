# Grand Line Escape

A top-down action-adventure game inspired by **One Piece**. Explore a hazardous island, fight pirates, collect keys and upgrades, defeat IMU, and escape. The game pairs a Java backend with a 60 FPS browser-based canvas client.

---

## Project Structure

```text
GrandLine-JavaBackend/
├── src/
│   ├── Main.java               # Runs boundary checks and starts the server
│   ├── GameServer.java         # Jetty HTTP API and WebSocket state stream
│   ├── GameEngine.java         # Simulation, world, combat, collisions, and snapshots
│   ├── Player.java             # Player stats, movement state, weapons, and upgrades
│   ├── Weapon.java             # Weapon stats, ammo, reload, and rarity upgrades
│   ├── PirateEnemy.java        # Pirate enemies, guardians, and IMU
│   ├── RarityChest.java        # Chests and their rewards
│   ├── SectorGate.java         # Key-locked progression gates
│   ├── WorldObstacle.java      # Solid obstacles and collision resolution
│   ├── LootDrop.java           # Physical item pickups
│   ├── Projectile.java         # Player and enemy projectiles
│   ├── SkyLaser.java           # IMU's laser attacks
│   └── BoundaryTestSuite.java  # Gameplay and input-validation tests
├── pom.xml                     # Maven build and Jetty WebSocket dependencies
├── web/
│   └── index.html              # Canvas game, controls, HUD, audio, and menus
├── Dockerfile                  # Container build
├── run.bat                     # Windows build and launch script
├── run.sh                      # macOS / Linux build and launch script
└── README.md                   # Project and gameplay guide
```

## Game Features

- **Explore five island sections** with solid crates, trees, gates, chests, and pirate encounters.
- **Fight and upgrade:** aim and fire weapons, dash to evade, reload, earn berries, buy weapons, and upgrade weapon rarity.
- **Collect and progress:** gather Log Poses to open section gates, and use chests for health, energy, and berries.
- **Explore separate realms:** challenge three Gold Key Guardians to collect the keys for the Secret Poneglyph Vault; defeat the Boss Card Warden to unlock the route to IMU's Empty Throne realm.
- **Awaken Gear 5** in the vault for increased damage, higher maximum health, and faster energy regeneration.
- **Complete the escape:** defeat IMU, collect the Escape Key, return to the Thousand Sunny, and interact with the ship.

## Technical Overview

The browser renders at 60 FPS and predicts player movement locally for responsive controls. Movement and position updates are sent over a persistent WebSocket. The Java server validates collisions, combat, damage, and progression, advances the game at 30 ticks per second, and pushes state snapshots 15 times per second. Game start and discrete actions use HTTP; the client reconnects and temporarily falls back to HTTP if the WebSocket disconnects.

The server uses Jetty and reads its listening port from the `PORT` environment variable, defaulting to `8080`. The `/health` endpoint provides a basic health check.

## Build and Run

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

Once started, open **`http://localhost:8080`** in your browser (it opens automatically on most desktops). To run the gameplay checks after building, use `java -cp target/classes BoundaryTestSuite`.

The game also includes an original synthesized soundtrack and sound effects. Use the **Audio** toggle to mute them.

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
