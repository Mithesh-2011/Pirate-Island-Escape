import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    public static final double ENEMY_RESPAWN_SECONDS = 180.0;

    public static final String[] DIFFICULTY_NAMES = {
        "Easy", "Normal", "Hard", "Expert"
    };
    public static final double[] DIFFICULTY_HP_MULT = {0.35, 0.75, 1.20, 2.00};
    public static final double[] DIFFICULTY_DMG_MULT = {0.20, 0.325, 0.60, 1.10};
    public static final int[] DIFFICULTY_GATE_REQ = {1, 2, 3, 4};
    public static final double LOG_POSE_DROP_CHANCE = 0.50;
    private static final int SECRET_VAULT_GOLD_KEY_REQUIREMENT = 3;
    private static final double ENEMY_FIRE_RATE_MULTIPLIER = 1.0 / 0.70;
    private static final String MAIN_REALM = "MAIN";
    private static final String GOLD_KEY_REALM = "GOLD_KEY";
    private static final String IMU_REALM = "IMU";

    private String gameMode;
    private String currentRealm;
    private int difficultyIndex;
    private double elapsedTime;

    private int enemiesDefeatedCount;
    private int chestsOpenedCount;
    private boolean imuDefeated;
    private boolean secretVaultUnlocked;
    private boolean gear5Unlocked;

    private boolean keyUp, keyDown, keyLeft, keyRight;
    private boolean firingHeld;

    private Player player;
    private final List<WorldObstacle> walls;
    private final List<SectorGate> gates;
    private final List<PortalRoute> portals;
    private final List<RarityChest> chests;
    private final List<PirateEnemy> enemies;
    private final List<LootDrop> lootDrops;
    private final List<Projectile> projectiles;
    private final List<SkyLaser> lasers;
    private final List<String> recentEvents;
    private final List<PendingEnemyRespawn> pendingEnemyRespawns;
    private int nextEventId;
    private int nextEnemyId;

    private static final class PendingEnemyRespawn {
        private final PirateEnemy enemy;
        private double remainingSeconds;

        private PendingEnemyRespawn(PirateEnemy enemy, double remainingSeconds) {
            this.enemy = enemy;
            this.remainingSeconds = remainingSeconds;
        }
    }

    private static final class PortalRoute {
        private final String id;
        private final String name;
        private final String realm;
        private final String destinationRealm;
        private final double x;
        private final double y;
        private final double destinationX;
        private final double destinationY;

        private PortalRoute(String id, String name, String realm, String destinationRealm,
                            double x, double y, double destinationX, double destinationY) {
            this.id = id;
            this.name = name;
            this.realm = realm;
            this.destinationRealm = destinationRealm;
            this.x = x;
            this.y = y;
            this.destinationX = destinationX;
            this.destinationY = destinationY;
        }
    }

    public GameEngine() {
        this.walls = new ArrayList<>();
        this.gates = new ArrayList<>();
        this.portals = new ArrayList<>();
        this.chests = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.lootDrops = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.lasers = new ArrayList<>();
        this.recentEvents = new ArrayList<>();
        this.pendingEnemyRespawns = new ArrayList<>();
        this.difficultyIndex = 1;
        resetGame("Normal", "Monkey D. Luffy");
        this.gameMode = "START";
    }

    public synchronized void resetGame(String difficultyName, String captainName) {
        this.difficultyIndex = resolveDifficultyIndex(difficultyName);
        this.gameMode = "PLAYING";
        this.currentRealm = MAIN_REALM;
        this.elapsedTime = 0.0;

        this.enemiesDefeatedCount = 0;
        this.chestsOpenedCount = 0;
        this.imuDefeated = false;
        this.secretVaultUnlocked = false;
        this.gear5Unlocked = false;

        this.keyUp = false;
        this.keyDown = false;
        this.keyLeft = false;
        this.keyRight = false;
        this.firingHeld = false;

        this.walls.clear();
        this.gates.clear();
        this.portals.clear();
        this.chests.clear();
        this.enemies.clear();
        this.lootDrops.clear();
        this.pendingEnemyRespawns.clear();
        this.projectiles.clear();
        this.lasers.clear();
        this.recentEvents.clear();
        this.nextEventId = 1;
        this.nextEnemyId = 1;

        this.player = new Player(captainName, 520.0, 1300.0);
        buildOpenIslandWorld();
        pushEvent("Set sail on " + DIFFICULTY_NAMES[difficultyIndex] + " difficulty!", "#38bdf8", player.getX(), player.getY() - 30);
    }

    public int resolveDifficultyIndex(String diffName) {
        if (diffName == null) {
            return 1;
        }
        for (int i = 0; i < DIFFICULTY_NAMES.length; i++) {
            if (DIFFICULTY_NAMES[i].equals(diffName)) {
                return i;
            }
        }
        return 1;
    }

    private void buildOpenIslandWorld() {
        int gateReq = DIFFICULTY_GATE_REQ[difficultyIndex];
        double hpMult = DIFFICULTY_HP_MULT[difficultyIndex];
        double dmgMult = DIFFICULTY_DMG_MULT[difficultyIndex];

        walls.add(new WorldObstacle(260, 420, 5100, 40, "cliff"));
        walls.add(new WorldObstacle(260, 2140, 5100, 40, "cliff"));
        walls.add(new WorldObstacle(260, 420, 40, 1760, "cliff"));
        walls.add(new WorldObstacle(5360, 420, 40, 1760, "cliff"));

        walls.add(new WorldObstacle(1740, 460, 38, 750, "fortress"));
        walls.add(new WorldObstacle(1740, 1390, 38, 750, "fortress"));
        gates.add(new SectorGate("gate1", "Sector 1 Sea-Gate", 1740, 1210, 38, 180, gateReq, 0, "#22c55e"));

        addSectorBarrier(2690);
        gates.add(new SectorGate("gate2", "Sector 2 Coral Pass", 2690, 1210, 38, 180,
                gateReq, 0, "#c27a42"));
        addSectorBarrier(3590);
        gates.add(new SectorGate("gate3", "Sector 3 Poneglyph Gate", 3590, 1210, 38, 180,
                gateReq + 1, 0, "#3b82f6"));
        addSectorBarrier(4540);
        gates.add(new SectorGate("gate4", "Sector 4 Red-Line Gate", 4540, 1210, 38, 180,
                gateReq + 1, 0, "#a74433"));
        walls.add(new WorldObstacle(2960, 460, 28, 210, "ruin"));
        walls.add(new WorldObstacle(3252, 460, 28, 210, "ruin"));
        walls.add(new WorldObstacle(2960, 645, 105, 28, "ruin"));
        walls.add(new WorldObstacle(3175, 645, 105, 28, "ruin"));
        gates.add(new SectorGate("secret_vault", "Secret Poneglyph Vault", 3065, 645, 110, 28,
                0, SECRET_VAULT_GOLD_KEY_REQUIREMENT, "#eab308"));

        addSectionObstacles();

        addRealmWalls(6500, 600, 1300, 1400, "gold_realm_wall");
        addRealmWalls(8700, 600, 1900, 1600, "imu_realm_wall");
        portals.add(new PortalRoute("gold_key_entry", "Gold Key Realm Portal", MAIN_REALM,
                GOLD_KEY_REALM, 2340, 1300, 7100, 1500));
        portals.add(new PortalRoute("gold_key_exit", "Return to Section 2", GOLD_KEY_REALM,
                MAIN_REALM, 7100, 1800, 2340, 1300));
        portals.add(new PortalRoute("imu_entry", "IMU Portal", MAIN_REALM,
                IMU_REALM, 5100, 1300, 9300, 1800));
        portals.add(new PortalRoute("imu_exit", "Return to Section 5", IMU_REALM,
                MAIN_REALM, 9600, 1900, 5100, 1300));

        chests.add(new RarityChest(700, 1400, 0));

        double[][] chestSections = {
            {900, 1680}, {1810, 2640}, {2730, 3560}, {3630, 4510}, {4560, 5260}
        };
        for (int section = 0; section < chestSections.length; section++) {
            int chestCount = section == 2 ? 6 : 5;
            for (int i = 0; i < chestCount; i++) {
                int rarity = section == 4 && Math.random() < 0.20
                        ? 3 : RarityChest.rollWeightedRarity();
                spawnChestAtValidLocation(chestSections[section][0], chestSections[section][1],
                        560, 2040, rarity);
            }
        }

        spawnPirates(gateReq, hpMult, dmgMult);
        addEnemy("gold_guardian", "Gold Key Guardian I", 6770, 980, 30, 620, 120, 34, 1.35,
                390, 35, 0, 1, "#8b5a16", "#facc15", false, hpMult, dmgMult);
        addEnemy("gold_guardian", "Gold Key Guardian II", 7310, 1280, 34, 780, 112, 42, 1.20,
                430, 42, 0, 1, "#6f4514", "#fde047", false, hpMult, dmgMult);
        addEnemy("gold_guardian", "Gold Key Guardian III", 7570, 850, 38, 960, 102, 52, 1.05,
                470, 50, 0, 1, "#5b3712", "#fbbf24", false, hpMult, dmgMult);
        addEnemy("imu", "IMU — Sovereign of the Empty Throne", 9800, 1080, 46, 2400,
                118, 34, 1.35, 680, 150, 0, 0, "#090d16", "#e11d48", true, hpMult, dmgMult);
        chests.add(new RarityChest(9250, 1550, 3));
        chests.add(new RarityChest(10300, 1550, 4));

    }

    private void addSectionObstacles() {
        double[][] sections = {
            {840, 1740}, {1778, 2690}, {2728, 3590}, {3628, 4540}, {4578, 5360}
        };
        double[] obstacleRows = {740, 1040, 1480, 1840};
        String[] materials = {"crate", "tree"};
        for (int section = 0; section < sections.length; section++) {
            double minX = sections[section][0];
            double width = sections[section][1] - minX;
            for (int row = 0; row < obstacleRows.length; row++) {
                for (int column = 0; column < 5; column++) {
                    double stagger = row % 2 == 0 ? -width / 24.0 : width / 24.0;
                    double centerX = minX + width * (column + 1) / 6.0 + stagger;
                    double centerY = obstacleRows[row];
                    String type = materials[(column + row + section) % materials.length];
                    if ("tree".equals(type)) {
                        walls.add(new WorldObstacle(centerX - 17, centerY - 12, 34, 32, type));
                    } else {
                        walls.add(new WorldObstacle(centerX - 33, centerY - 30, 66, 60, type));
                    }
                }
            }
        }
    }

    private void addRealmWalls(double x, double y, double width, double height, String type) {
        walls.add(new WorldObstacle(x, y, width, 40, type));
        walls.add(new WorldObstacle(x, y + height - 40, width, 40, type));
        walls.add(new WorldObstacle(x, y, 40, height, type));
        walls.add(new WorldObstacle(x + width - 40, y, 40, height, type));
    }

    private void addSectorBarrier(double x) {
        walls.add(new WorldObstacle(x, 460, 38, 750, "fortress"));
        walls.add(new WorldObstacle(x, 1390, 38, 750, "fortress"));
    }

    private double mapEnemyX(double originalX) {
        if (originalX < 1740) return originalX;
        if (originalX < 2690) return originalX + 900;
        return originalX + 1800;
    }

    private void spawnChestAtValidLocation(double minX, double maxX, double minY, double maxY, int rarityIdx) {
        for (int attempts = 0; attempts < 60; attempts++) {
            double cx = minX + Math.random() * (maxX - minX);
            double cy = minY + Math.random() * (maxY - minY);
            if (isChestLocationAvailable(cx, cy)) {
                chests.add(new RarityChest(cx, cy, rarityIdx));
                return;
            }
        }

        for (double cy = minY + 36; cy <= maxY - 36; cy += 72) {
            for (double cx = minX + 36; cx <= maxX - 36; cx += 72) {
                if (isChestLocationAvailable(cx, cy)) {
                    chests.add(new RarityChest(cx, cy, rarityIdx));
                    return;
                }
            }
        }
        throw new IllegalStateException("Unable to find a valid chest location in the requested area");
    }

    private boolean isChestLocationAvailable(double cx, double cy) {
        for (RarityChest chest : chests) {
            if (Math.hypot(cx - chest.getX(), cy - chest.getY()) < 76.0) {
                return false;
            }
        }
        for (WorldObstacle wall : walls) {
            if (cx + 36 > wall.getX() && cx - 36 < wall.getX() + wall.getWidth()
                    && cy + 36 > wall.getY() && cy - 36 < wall.getY() + wall.getHeight()) {
                return false;
            }
        }
        return true;
    }

    private boolean overlapsSectionObstacle(double x, double y, double radius) {
        for (WorldObstacle wall : walls) {
            if (!"crate".equals(wall.getMaterialType()) && !"tree".equals(wall.getMaterialType())) {
                continue;
            }
            double closestX = Math.max(wall.getX(), Math.min(x, wall.getX() + wall.getWidth()));
            double closestY = Math.max(wall.getY(), Math.min(y, wall.getY() + wall.getHeight()));
            double dx = x - closestX;
            double dy = y - closestY;
            if (dx * dx + dy * dy < radius * radius) {
                return true;
            }
        }
        return false;
    }

    private double[] findEnemySpawnPosition(double x, double y, double radius) {
        double sectionMinX = Double.NEGATIVE_INFINITY;
        double sectionMaxX = Double.POSITIVE_INFINITY;
        if (x >= 260.0 && x < 5360.0) {
            double[] sectionEnds = {1740.0, 2690.0, 3590.0, 4540.0, 5360.0};
            sectionMinX = 260.0;
            for (double sectionEnd : sectionEnds) {
                sectionMaxX = sectionEnd;
                if (x < sectionEnd) {
                    break;
                }
                sectionMinX = sectionEnd;
            }
        }

        for (int distance = 0; distance <= 240; distance += 20) {
            int angleSteps = distance == 0 ? 1 : 16;
            for (int angle = 0; angle < angleSteps; angle++) {
                double radians = Math.PI * 2.0 * angle / angleSteps;
                double candidateX = x + Math.cos(radians) * distance;
                double candidateY = y + Math.sin(radians) * distance;
                if (candidateX - radius < sectionMinX || candidateX + radius >= sectionMaxX) {
                    continue;
                }
                if (!overlapsSectionObstacle(candidateX, candidateY, radius)) {
                    return new double[]{candidateX, candidateY};
                }
            }
        }
        throw new IllegalStateException("Unable to find a clear enemy spawn near (" + x + ", " + y + ")");
    }

    private void spawnPirates(int gateReq, double hpMult, double dmgMult) {

        addEnemy("buggy", "Buggy Pirate", 1050, 820, 21, 65, 125, 12, 1.65, 340, 8, 1, 0, "#ef4444", "#38bdf8", false, hpMult, dmgMult);
        addEnemy("buggy", "Buggy Pirate", 1120, 1340, 21, 65, 125, 12, 1.65, 340, 8, 1, 0, "#ef4444", "#38bdf8", false, hpMult, dmgMult);
        addEnemy("alvida", "Alvida Bruiser", 1240, 1080, 26, 115, 98, 18, 1.90, 160, 11, 1, 0, "#ec4899", "#fde047", false, hpMult, dmgMult);
        addEnemy("buggy", "Buggy Pirate", 1350, 1680, 21, 65, 125, 12, 1.65, 340, 8, 1, 0, "#ef4444", "#38bdf8", false, hpMult, dmgMult);
        addEnemy("alvida", "Alvida Bruiser", 1520, 860, 26, 115, 98, 18, 1.90, 160, 11, 1, 0, "#ec4899", "#fde047", false, hpMult, dmgMult);
        addEnemy("buggy", "Buggy Pirate", 1580, 1320, 21, 65, 125, 12, 1.65, 340, 8, 1, 0, "#ef4444", "#38bdf8", false, hpMult, dmgMult);
        addEnemy("alvida", "Alvida Bruiser", 1620, 1740, 26, 115, 98, 18, 1.90, 160, 11, 1, 0, "#ec4899", "#fde047", false, hpMult, dmgMult);

        addEnemy("fishman", "Arlong Fish-Man", 1940, 840, 23, 140, 172, 20, 1.35, 260, 14, 1, 0, "#0ea5e9", "#f97316", false, hpMult, dmgMult);
        addEnemy("sniper", "Baroque Sniper", 2060, 1420, 20, 105, 105, 24, 2.10, 480, 15, 1, 0, "#6366f1", "#facc15", false, hpMult, dmgMult);
        addEnemy("elite_pink", "Elite Pink Captain", 1960, 1780, 27, 210, 142, 24, 1.25, 360, 25, 2, 1, "#f472b6", "#eab308", false, hpMult, dmgMult);
        addEnemy("fishman", "Arlong Fish-Man", 2240, 1040, 23, 140, 172, 20, 1.35, 260, 14, 1, 0, "#0ea5e9", "#f97316", false, hpMult, dmgMult);
        addEnemy("elite_pink", "Elite Pink Captain", 2340, 1320, 27, 210, 142, 24, 1.25, 360, 25, 2, 1, "#f472b6", "#eab308", false, hpMult, dmgMult);
        addEnemy("sniper", "Baroque Sniper", 2480, 860, 20, 105, 105, 24, 2.10, 480, 15, 1, 0, "#6366f1", "#facc15", false, hpMult, dmgMult);
        addEnemy("fishman", "Arlong Fish-Man", 2520, 1680, 23, 140, 172, 20, 1.35, 260, 14, 1, 0, "#0ea5e9", "#f97316", false, hpMult, dmgMult);
        addEnemy("elite_pink", "Elite Pink Captain", 2560, 1180, 27, 210, 142, 24, 1.25, 360, 25, 2, 1, "#f472b6", "#eab308", false, hpMult, dmgMult);

        addEnemy("cp0", "CP0 Masked Agent", 2840, 840, 22, 220, 192, 26, 1.15, 330, 22, 2, 0, "#f8fafc", "#1e293b", false, hpMult, dmgMult);
        addEnemy("admiral", "Navy Admiral", 2980, 1320, 30, 340, 125, 32, 1.40, 420, 34, 2, 1, "#eab308", "#dc2626", false, hpMult, dmgMult);
        addEnemy("cp0", "CP0 Masked Agent", 2890, 1760, 22, 220, 192, 26, 1.15, 330, 22, 2, 0, "#f8fafc", "#1e293b", false, hpMult, dmgMult);
        addEnemy("admiral", "Navy Admiral", 3160, 880, 30, 340, 125, 32, 1.40, 420, 34, 2, 1, "#eab308", "#dc2626", false, hpMult, dmgMult);
        addEnemy("cp0", "CP0 Masked Agent", 3220, 1660, 22, 220, 192, 26, 1.15, 330, 22, 2, 0, "#f8fafc", "#1e293b", false, hpMult, dmgMult);
        addEnemy("admiral", "Navy Admiral", 3320, 1120, 30, 340, 125, 32, 1.40, 420, 34, 2, 1, "#eab308", "#dc2626", false, hpMult, dmgMult);

        addAddedSectorEnemy("marine", "Coral Coast Marine", 1880, 900, 22, 90, 118, 18, 1.50, 320, 12, 0, "#c65a43", "#f5d0b5", hpMult, dmgMult);
        addAddedSectorEnemy("sniper", "Windward Lookout", 2110, 1650, 20, 120, 110, 22, 1.55, 460, 16, 0, "#714b3e", "#f2c39f", hpMult, dmgMult);
        addAddedSectorEnemy("bruiser", "Ironclad Raider", 2430, 1060, 29, 220, 92, 28, 1.45, 180, 26, 0, "#8e4937", "#e5a181", hpMult, dmgMult);
        addAddedSectorEnemy("marine", "Cinder Coast Marine", 3780, 900, 23, 180, 145, 28, 1.25, 350, 24, 0, "#9d3e31", "#f0b77e", hpMult, dmgMult);
        addAddedSectorEnemy("sniper", "Red-Line Sharpshooter", 4080, 1650, 21, 240, 128, 34, 1.20, 540, 30, 0, "#4d3834", "#e9c4a4", hpMult, dmgMult);
        addAddedSectorEnemy("admiral", "Cinder Coast Warden", 4380, 1120, 31, 390, 142, 38, 1.25, 440, 38, 0, "#c27a42", "#f4d18c", hpMult, dmgMult);

        ensureSectionEnemyCount(260, 1740, gateReq * 2, hpMult, dmgMult,
                new double[][] {{620, 740}, {860, 1840}, {1420, 640}, {1580, 1880}, {680, 1300}});
        ensureSectionEnemyCount(1778, 2690, gateReq * 2, hpMult, dmgMult,
                new double[][] {{1840, 700}, {2020, 1860}, {2220, 740}, {2500, 1860}, {2600, 980},
                        {1830, 1780}, {2260, 1880}, {2600, 740}, {2500, 980}});
        ensureSectionEnemyCount(2728, 3590, gateReq * 2, hpMult, dmgMult,
                new double[][] {{2760, 740}, {2880, 1900}, {3060, 760}, {3400, 1880},
                        {3500, 760}, {2780, 1840}, {3440, 980}});
        ensureSectionEnemyCount(3628, 4540, gateReq * 2, hpMult, dmgMult,
                new double[][] {{3680, 720}, {3860, 1880}, {4020, 740}, {4200, 1880}, {4480, 740},
                        {3700, 1840}, {3880, 720}, {4100, 1880}, {4300, 740}, {4480, 1880}, {3720, 980}});
        ensureSectionEnemyCount(4578, 5360, Math.max(0, gateReq * 2 - 1), hpMult, dmgMult,
                new double[][] {{4620, 740}, {4680, 1880}, {4900, 740}, {5200, 1840}, {5300, 740},
                        {4600, 1840}, {4820, 1880}, {5000, 740}, {5240, 1880}, {5320, 980}});
        addAddedSectorEnemy("imu_key_holder", "IMU Keycard Warden", 4860, 1300, 25, 300,
                120, 28, 1.35, 360, 28, 0, "#9f1239", "#fda4af", hpMult, dmgMult);

    }

    private void ensureSectionEnemyCount(double minX, double maxX, int targetCount,
                                         double hpMult, double dmgMult, double[][] spawnPoints) {
        int currentCount = 0;
        for (PirateEnemy enemy : enemies) {
            if (!enemy.isBoss() && enemy.getX() >= minX && enemy.getX() < maxX) {
                currentCount++;
            }
        }

        for (int i = enemies.size() - 1; currentCount > targetCount && i >= 0; i--) {
            PirateEnemy enemy = enemies.get(i);
            if (!enemy.isBoss() && enemy.getX() >= minX && enemy.getX() < maxX) {
                enemies.remove(i);
                currentCount--;
            }
        }

        String[] types = {"marine", "bruiser", "sniper", "fishman", "elite_pink"};
        String[] names = {"Ashen Deckhand", "Ironjaw Raider", "Dreadwake Lookout", "Reef Stalker", "Blackflag Captain"};
        String[] coats = {"#8f3027", "#49302a", "#514238", "#315b60", "#852e35"};
        String[] trims = {"#edb36e", "#c65a43", "#d4b28b", "#e29c68", "#e7b66c"};
        int missing = Math.min(targetCount - currentCount, spawnPoints.length);
        for (int i = 0; i < missing; i++) {
            double[] point = spawnPoints[i];
            int archetype = i % types.length;
            addAddedSectorEnemy(types[archetype], names[archetype], point[0], point[1],
                    21 + archetype % 3 * 2, 100 + archetype * 24, 112 + archetype * 8,
                    18 + archetype * 4, 1.5 - archetype * 0.05, 340 + archetype * 35,
                    12 + archetype * 4, 0, coats[archetype], trims[archetype], hpMult, dmgMult);
        }
    }

    private void addEnemy(String type, String name, double x, double y, double radius, int hp,
                          double speed, int dmg, double fireRate, double range, int berries,
                          int greenDrop, int goldDrop, String c1, String c2, boolean boss,
                          double hpMult, double dmgMult) {
        int pressure = difficultyIndex;
        double worldX = boss || "gold_guardian".equals(type) ? x : mapEnemyX(x);
        double[] spawn = findEnemySpawnPosition(worldX, y, radius);
        enemies.add(new PirateEnemy(nextEnemyId++, type, name, spawn[0], spawn[1], radius, hp,
                speed * 0.85 * (1.0 + pressure * 0.05), dmg,
                fireRate / (1.15 + pressure * 0.10) * ENEMY_FIRE_RATE_MULTIPLIER,
                range, berries, greenDrop, "gold_guardian".equals(type) ? goldDrop : 0,
                c1, c2, boss, hpMult, dmgMult));
    }

    private void addAddedSectorEnemy(String type, String name, double x, double y, double radius,
                                     int hp, double speed, int dmg, double fireRate, double range,
                                     int berries, int goldDrop, String c1, String c2,
                                     double hpMult, double dmgMult) {
        int pressure = difficultyIndex;
        double[] spawn = findEnemySpawnPosition(x, y, radius);
        enemies.add(new PirateEnemy(nextEnemyId++, type, name, spawn[0], spawn[1], radius, hp,
                speed * 0.85 * (1.0 + pressure * 0.05), dmg,
                fireRate / (1.15 + pressure * 0.10) * ENEMY_FIRE_RATE_MULTIPLIER,
                range, berries, 0, 0, c1, c2, false, hpMult, dmgMult));
    }

    public synchronized void updateInputState(boolean up, boolean down, boolean left, boolean right,
                                              boolean firing, double aimAngle) {
        this.keyUp = up;
        this.keyDown = down;
        this.keyLeft = left;
        this.keyRight = right;
        this.firingHeld = firing && player != null && !isPlayerInSafeHarbor();
        if (player != null) {
            player.setAimAngle(aimAngle);
        }
    }

    public synchronized void updateClientMovement(double x, double y,
                                                  java.util.Map<Integer, double[]> enemyPositions,
                                                  java.util.Map<Long, double[]> projectilePositions) {
        if (!"PLAYING".equals(gameMode) || player == null) {
            return;
        }
        if (Double.isFinite(x) && Double.isFinite(y)) {
            double[] resolved = resolveClientMovementPosition(
                    player.getX(), player.getY(), x, y, player.getRadius());
            player.setPosition(resolved[0], resolved[1]);
        }
        for (PirateEnemy enemy : enemies) {
            double[] position = enemyPositions.get(enemy.getId());
            if (position == null || !Double.isFinite(position[0]) || !Double.isFinite(position[1])) {
                continue;
            }
            double[] resolved = resolveClientMovementPosition(
                    enemy.getX(), enemy.getY(), position[0], position[1], enemy.getRadius());
            enemy.setPosition(resolved[0], resolved[1]);
        }
        for (Projectile projectile : projectiles) {
            double[] position = projectilePositions.get(projectile.getId());
            if (position != null && Double.isFinite(position[0]) && Double.isFinite(position[1])) {
                projectile.setPosition(position[0], position[1]);
            }
        }
    }

    public synchronized String executePlayerAction(String action, int paramValue) {
        if (action == null || player == null) {
            return "INVALID_ACTION";
        }

        if ("PAUSE".equals(action)) {
            if ("PLAYING".equals(gameMode)) {
                gameMode = "PAUSED";
                keyUp = keyDown = keyLeft = keyRight = firingHeld = false;
                return "PAUSED";
            }
            if ("PAUSED".equals(gameMode)) {
                gameMode = "PLAYING";
                return "RESUMED";
            }
            return "NOT_PLAYING";
        } else if ("RESUME".equals(action) && "PAUSED".equals(gameMode)) {
            gameMode = "PLAYING";
            return "RESUMED";
        }

        if (!"PLAYING".equals(gameMode)) {
            return "NOT_PLAYING";
        }

        if ("FIRE".equals(action)) {
            if (isPlayerInSafeHarbor()) {
                return "SAFE_HARBOR";
            }
            Weapon weapon = player.getEquippedWeapon();
            if (weapon.isReloading()) {
                return "RELOADING";
            }
            if (!weapon.canFire()) {
                if (weapon.isUnlocked() && weapon.getCurrentAmmo() <= 0) {
                    weapon.startReload();
                    return "NO_AMMO";
                }
                return "CANNOT_FIRE";
            }
            if (player.getEnergy() < Weapon.ENERGY_COST_PER_SHOT) {
                pushEvent("Need " + (int) Weapon.ENERGY_COST_PER_SHOT + " Energy to fire",
                        "#38bdf8", player.getX(), player.getY() - 26);
                return "LOW_ENERGY";
            }
            return fireEquippedWeapon() ? "FIRED" : "CANNOT_FIRE";
        } else if ("DASH".equals(action)) {
            double dx = (keyRight ? 1.0 : 0.0) - (keyLeft ? 1.0 : 0.0);
            double dy = (keyDown ? 1.0 : 0.0) - (keyUp ? 1.0 : 0.0);
            if (player.tryDash(dx, dy)) {
                pushEvent("GUM-GUM ROCKET!", "#38bdf8", player.getX(), player.getY() - 26);
                return "DASHED";
            } else {
                pushEvent("Need 25 Energy! (Open Chests)", "#38bdf8", player.getX(), player.getY() - 26);
                return "LOW_ENERGY";
            }
        } else if ("RELOAD".equals(action)) {
            player.getEquippedWeapon().startReload();
            return "RELOADING";
        } else if ("SELECT_SLOT".equals(action)) {
            return player.selectWeaponSlot(paramValue) ? "SLOT_SELECTED" : "SLOT_LOCKED";
        } else if ("BUY_WEAPON".equals(action)) {
            if (player.buyWeapon(paramValue)) {
                Weapon w = player.getWeapons().get(paramValue);
                pushEvent("Unlocked " + w.getName() + "!", "#22c55e", player.getX(), player.getY() - 28);
                return "PURCHASED";
            }
            return "CANNOT_BUY";
        } else if ("UPGRADE_WEAPON".equals(action)) {
            if (player.upgradeWeaponRarity(paramValue)) {
                Weapon w = player.getWeapons().get(paramValue);
                pushEvent("Upgraded to " + w.getRarityName() + "!", w.getRarityColor(), player.getX(), player.getY() - 28);
                return "UPGRADED";
            }
            return "CANNOT_UPGRADE";
        } else if ("INTERACT".equals(action)) {
            return handleInteract();
        } else if ("RETURN_TO_BASE".equals(action)) {
            if (isPlayerInSafeHarbor()) {
                return "ALREADY_AT_BASE";
            }
            if (player.getGreenLogPoses() < 1) {
                pushEvent("Need 1 Green Log Pose to return to base",
                        "#86efac", player.getX(), player.getY() - 26);
                return "NO_LOG_POSE";
            }
            player.spendGreenLogPoses(1);
            currentRealm = MAIN_REALM;
            player.setPosition(500, 1300);
            projectiles.clear();
            lasers.clear();
            pushEvent("Returned to base. 1 Log Pose spent.", "#86efac", player.getX(), player.getY() - 26);
            return "RETURNED_TO_BASE";
        }
        return "UNKNOWN_ACTION";
    }

    private String handleInteract() {
        for (PortalRoute portal : portals) {
            if (portal.realm.equals(currentRealm) && player.distanceTo(portal.x, portal.y) <= 92.0) {
                if ("imu_entry".equals(portal.id) && player.getImuKeycards() < 1) {
                    pushEvent("Find the IMU Keycard in section 5 first!",
                            "#fda4af", player.getX(), player.getY() - 28);
                    return "GATE_LOCKED";
                }
                if ("imu_entry".equals(portal.id)) {
                    player.spendImuKeycards(1);
                }
                currentRealm = portal.destinationRealm;
                player.setPosition(portal.destinationX, portal.destinationY);
                projectiles.clear();
                lasers.clear();
                pushEvent("Entered " + portal.name, "#a78bfa", player.getX(), player.getY() - 28);
                return "PORTAL_TRAVELLED";
            }
        }

        for (int i = 0; i < chests.size(); i++) {
            RarityChest c = chests.get(i);
            if (!c.isOpened() && player.distanceTo(c.getX(), c.getY()) <= 58.0) {
                openChest(c);
                return "CHEST_OPENED";
            }
        }

        for (int i = 0; i < gates.size(); i++) {
            SectorGate g = gates.get(i);
            if (!g.isOpened() && player.distanceTo(g.getCenterX(), g.getCenterY()) <= 125.0) {
                if (g.canUnlock(player.getGreenLogPoses(), player.getGoldKeycards(),
                        player.getImuKeycards())) {
                    if (g.getRequiredSpecialKeys() > 0) {
                        player.spendImuKeycards(g.getRequiredSpecialKeys());
                    } else if (g.getRequiredGoldKeys() > 0) {
                        player.spendGoldKeycards(g.getRequiredGoldKeys());
                        secretVaultUnlocked = true;
                    } else {
                        player.spendGreenLogPoses(g.getRequiredGreenKeys());
                    }
                    g.setOpened(true);
                    pushEvent(g.getName() + " UNLOCKED!", "#22c55e", g.getCenterX(), g.getCenterY() - 20);
                    return "GATE_UNLOCKED";
                } else {
                    String msg = (g.getRequiredSpecialKeys() > 0)
                            ? "Need " + g.getRequiredSpecialKeys() + " IMU Keycard!"
                            : (g.getRequiredGoldKeys() > 0)
                            ? "Need " + g.getRequiredGoldKeys() + " Gold Keycards!"
                            : "Need " + g.getRequiredGreenKeys() + " Green Log Poses!";
                    pushEvent(msg, "#f87171", g.getCenterX(), g.getCenterY() - 20);
                    return "GATE_LOCKED";
                }
            }
        }

        if (!gear5Unlocked && player.distanceTo(3120, 540) <= 78.0) {
            if (!secretVaultUnlocked) {
                pushEvent("Unlock the Secret Poneglyph Vault with 3 Gold Keys first!",
                        "#fbbf24", 3120, 510);
                return "SECRET_LOCKED";
            }
            gear5Unlocked = true;
            player.activateGear5();
            pushEvent("GEAR 5 AWAKENED! +40% DAMAGE, 150 MAX HP, 2 ENERGY/SEC",
                    "#f43f5e", 3120, 510);
            return "SECRET_CLAIMED";
        }

        if (MAIN_REALM.equals(currentRealm) && player.distanceTo(450, 1300) <= 104.0) {
            if (!imuDefeated) {
                pushEvent("Defeat IMU on the Empty Throne first!", "#fbbf24", 450, 1240);
                return "BOSS_ALIVE";
            }
            if (player.hasEscapeKey()) {
                gameMode = "VICTORY";
                pushEvent("GRAND LINE ESCAPED! VICTORY!", "#fbbf24", 450, 1240);
                return "VICTORY";
            } else {
                pushEvent("Collect IMU's Escape Key first!", "#fbbf24", 450, 1240);
                return "NO_ESCAPE_KEY";
            }
        }

        return "NO_TARGET";
    }

    private void openChest(RarityChest c) {
        c.markOpened(40.0);
        chestsOpenedCount++;
        int idx = c.getRarityIndex();
        int hpGain = c.getHpRestore();
        int enGain = c.getEnergyRestore();
        int berryGain = c.getBerryReward();

        player.applyChestRewards(hpGain, enGain, berryGain, idx == 4);
        String rarityName = Weapon.RARITY_NAMES[idx];
        String rarityColor = Weapon.RARITY_COLORS[idx];
        StringBuilder sb = new StringBuilder();
        sb.append("+").append(hpGain).append(" HP  +").append(enGain).append(" EN  +B").append(berryGain);

        if (idx >= 3 || (idx == 2 && Math.random() < 0.45)) {
            player.addGreenLogPoses(1);
            sb.append("  +1 LogPose");
        }
        pushEvent(rarityName.toUpperCase() + " CHEST! " + sb.toString(), rarityColor, c.getX(), c.getY() - 22);
    }

    private boolean fireEquippedWeapon() {
        if (isPlayerInSafeHarbor()) {
            return false;
        }
        Weapon w = player.getEquippedWeapon();
        if (!w.canFire()) {
            if (w.isUnlocked() && !w.isReloading() && w.getCurrentAmmo() <= 0) {
                w.startReload();
            }
            return false;
        }
        if (!player.consumeEnergy(Weapon.ENERGY_COST_PER_SHOT)) {
            return false;
        }

        w.consumeShot();
        int scaledDmg = (int) Math.round(w.getScaledDamage() * player.getDamageMultiplier());
        double muzzleDist = player.getRadius() + 12.0;
        double mx = player.getX() + Math.cos(player.getAimAngle()) * muzzleDist;
        double my = player.getY() + Math.sin(player.getAimAngle()) * muzzleDist;

        int pellets = w.getPelletCount();
        for (int i = 0; i < pellets; i++) {
            double offset = (pellets == 1) ? 0.0 : (i - (pellets - 1) / 2.0) * w.getSpreadAngle();
            double ang = player.getAimAngle() + offset + (Math.random() - 0.5) * 0.02;
            projectiles.add(new Projectile(
                    mx, my,
                    Math.cos(ang) * w.getProjectileSpeed(),
                    Math.sin(ang) * w.getProjectileSpeed(),
                    w.getProjectileRadius(),
                    scaledDmg,
                    w.getProjectileLife(),
                    true,
                    w.isPiercing(),
                    w.getProjectileColor(),
                    w.getRarityColor()
            ));
        }
        return true;
    }

    public synchronized void tick(double dt) {
        if (!"PLAYING".equals(gameMode) || player == null) {
            return;
        }

        elapsedTime += dt;
        player.regeneratePassiveEnergy(dt);
        player.tickTimers(dt);

        if (firingHeld && !isPlayerInSafeHarbor()) {
            fireEquippedWeapon();
        }

        for (int i = 0; i < chests.size(); i++) {
            RarityChest c = chests.get(i);
            c.update(dt);
        }

        updateEnemies(dt);
        updatePendingEnemyRespawns(dt);

        updateProjectiles(dt);
        enemies.removeIf(enemy -> !enemy.isAlive());

        updateLootDrops(dt);

        updateLasers(dt);
    }

    private void updateLootDrops(double dt) {
        for (int i = lootDrops.size() - 1; i >= 0; i--) {
            LootDrop drop = lootDrops.get(i);
            drop.update(dt);
            if (player.distanceTo(drop.getX(), drop.getY()) > 52.0) {
                continue;
            }
            if (drop.isBerry()) {
                player.addBerries(drop.getAmount());
                pushEvent("+B" + drop.getAmount(), "#fbbf24", drop.getX(), drop.getY() - 16);
            } else if ("LOG_POSE".equals(drop.getType())) {
                player.addGreenLogPoses(drop.getAmount());
                pushEvent("+" + drop.getAmount() + " LOG POSE", "#86efac", drop.getX(), drop.getY() - 16);
            } else if ("GOLD_KEY".equals(drop.getType())) {
                player.addGoldKeycard();
                pushEvent("+1 GOLD KEY", "#facc15", drop.getX(), drop.getY() - 16);
            } else if ("IMU_KEYCARD".equals(drop.getType())) {
                player.addImuKeycard();
                pushEvent("+1 IMU KEYCARD", "#fda4af", drop.getX(), drop.getY() - 16);
                } else if ("ESCAPE_KEY".equals(drop.getType())) {
                    player.addEscapeKey();
                    pushEvent("ESCAPE KEY ACQUIRED! RETURN TO THE THOUSAND SUNNY!",
                            "#fbbf24", drop.getX(), drop.getY() - 16);
            }
            lootDrops.remove(i);
        }
    }

    private void updatePendingEnemyRespawns(double dt) {
        for (int i = 0; i < pendingEnemyRespawns.size();) {
            PendingEnemyRespawn pending = pendingEnemyRespawns.get(i);
            pending.remainingSeconds -= dt;
            if (pending.remainingSeconds <= 0.0) {
                PirateEnemy respawned = pending.enemy.respawnAs(nextEnemyId++);
                double[] spawn = findEnemySpawnPosition(
                        respawned.getX(), respawned.getY(), respawned.getRadius());
                respawned.setPosition(spawn[0], spawn[1]);
                enemies.add(respawned);
                pushEvent(respawned.getName() + " returned to battle!", "#fb7185",
                        respawned.getX(), respawned.getY() - 24);
                pendingEnemyRespawns.remove(i);
            } else {
                i++;
            }
        }
    }

    private double[] resolveEntityPosition(double px, double py, double r) {
        for (int i = 0; i < walls.size(); i++) {
            double[] res = walls.get(i).resolveCircleCollision(px, py, r);
            px = res[0];
            py = res[1];
        }
        for (int i = 0; i < gates.size(); i++) {
            SectorGate g = gates.get(i);
            if (!g.isOpened()) {
                double[] res = resolveCircleAgainstRectangle(px, py, r, g.getX(), g.getY(),
                        g.getWidth(), g.getHeight());
                px = res[0];
                py = res[1];
            }
        }
        return new double[]{px, py};
    }

    private double[] resolveClientMovementPosition(double oldX, double oldY, double targetX,
                                                   double targetY, double radius) {
        double distance = Math.hypot(targetX - oldX, targetY - oldY);
        if (!Double.isFinite(distance) || distance > 250.0 || distance < 0.5) {
            return new double[]{oldX, oldY};
        }
        int steps = Math.max(1, (int) Math.ceil(distance / 8.0));
        double x = oldX;
        double y = oldY;
        double stepX = (targetX - oldX) / steps;
        double stepY = (targetY - oldY) / steps;
        for (int step = 1; step <= steps; step++) {
            double[] resolved = resolveEntityPosition(x + stepX, y + stepY, radius);
            x = resolved[0];
            y = resolved[1];
        }
        return new double[]{x, y};
    }

    private double[] resolveCircleAgainstRectangle(double cx, double cy, double radius,
                                                    double x, double y, double width, double height) {
        double closestX = Math.max(x, Math.min(cx, x + width));
        double closestY = Math.max(y, Math.min(cy, y + height));
        double dx = cx - closestX;
        double dy = cy - closestY;
        double distanceSquared = dx * dx + dy * dy;
        if (distanceSquared < radius * radius) {
            double distance = Math.sqrt(distanceSquared);
            if (distance > 0.0001) {
                double overlap = radius - distance;
                cx += dx / distance * overlap;
                cy += dy / distance * overlap;
            } else {
                double left = cx - x;
                double right = x + width - cx;
                double top = cy - y;
                double bottom = y + height - cy;
                double nearest = Math.min(Math.min(left, right), Math.min(top, bottom));
                if (nearest == left) {
                    cx = x - radius;
                } else if (nearest == right) {
                    cx = x + width + radius;
                } else if (nearest == top) {
                    cy = y - radius;
                } else {
                    cy = y + height + radius;
                }
            }
        }
        return new double[]{cx, cy};
    }

    private void updateEnemies(double dt) {
        for (int i = 0; i < enemies.size(); i++) {
            PirateEnemy en = enemies.get(i);
            if (!en.isAlive()) {
                continue;
            }
            if (!isEnemyInCurrentRealm(en)) {
                continue;
            }
            if (isPlayerInSafeHarbor()) {
                continue;
            }

            en.tickTimers(dt);
            double dx = player.getX() - en.getX();
            double dy = player.getY() - en.getY();
            double dist = Math.hypot(dx, dy);
            en.setAimAngle(Math.atan2(dy, dx));

            double aggroRange = en.isBoss() ? 920.0 : 480.0;
            if (dist < aggroRange) {
                if (en.getAttackTimer() <= 0.0 && dist <= en.getAttackRange() + 60.0) {
                    en.resetAttackTimer();
                    if (en.isBoss()) {
                        int phase = en.getBossPhase();
                        int shots = (phase >= 2) ? 7 : 5;
                        for (int s = 0; s < shots; s++) {
                            double ang = en.getAimAngle() + (s - (shots - 1) / 2.0) * 0.22;
                            projectiles.add(new Projectile(
                                    en.getX(), en.getY(),
                                    Math.cos(ang) * 430.0, Math.sin(ang) * 430.0,
                                    10.0, en.getDamage(), 2.2, false, false, "#e11d48", "#fb7185"
                            ));
                        }
                    } else {
                        double spd = "sniper".equals(en.getType()) ? 560.0 : 370.0;
                        double rad = "admiral".equals(en.getType()) ? 11.0 : 7.0;
                        projectiles.add(new Projectile(
                                en.getX(), en.getY(),
                                Math.cos(en.getAimAngle()) * spd, Math.sin(en.getAimAngle()) * spd,
                                rad, en.getDamage(), 1.8, false, false, en.getAccentColor(), en.getPrimaryColor()
                        ));
                    }
                }

                if (en.isBoss() && en.getSpecialTimer() <= 0.0) {
                    int phase = en.getBossPhase();
                    en.resetSpecialTimer(phase >= 2 ? 3.2 : 4.4);
                    for (int k = 0; k < 3; k++) {
                        double rx = player.getX() + (Math.random() - 0.5) * 320.0;
                        double ry = player.getY() + (Math.random() - 0.5) * 320.0;
                        lasers.add(new SkyLaser(rx, ry - 260, rx, ry + 20, 2.3, 0.0, 1.25, 0.75, 34, true,
                                (int) Math.round(en.getDamage() * 1.25)));
                    }
                }
            }

        }
    }

    private boolean isEnemyInCurrentRealm(PirateEnemy enemy) {
        if (GOLD_KEY_REALM.equals(currentRealm)) {
            return "gold_guardian".equals(enemy.getType());
        }
        if (IMU_REALM.equals(currentRealm)) {
            return enemy.isBoss();
        }
        return !enemy.isBoss() && !"gold_guardian".equals(enemy.getType());
    }

    private void updateProjectiles(double dt) {
        int i = 0;
        while (i < projectiles.size()) {
            Projectile pr = projectiles.get(i);
            pr.advanceLifetime(dt);

            if (pr.isExpired() || hitsAnyWallOrClosedGate(pr.getX(), pr.getY())
                    || (pr.isFromPlayer() && isPlayerInSafeHarbor())) {
                projectiles.remove(i);
                continue;
            }

            boolean removed = false;
            if (pr.isFromPlayer()) {
                for (int j = 0; j < enemies.size(); j++) {
                    PirateEnemy en = enemies.get(j);
                    if (!en.isAlive() || !isEnemyInCurrentRealm(en) || pr.hasAlreadyHit(en.getId())) {
                        continue;
                    }
                    double dist = Math.hypot(pr.getX() - en.getX(), pr.getY() - en.getY());
                    if (dist < pr.getRadius() + en.getRadius()) {
                        en.takeDamage(pr.getDamage());
                        pushEvent("-" + pr.getDamage(), "#fde047", en.getX(), en.getY() - en.getRadius() - 8);

                        if (!en.isAlive()) {
                            handleEnemyDefeated(en);
                        }

                        if (pr.isPiercing()) {
                            pr.recordHit(en.getId());
                        } else {
                            projectiles.remove(i);
                            removed = true;
                        }
                        break;
                    }
                }
            } else {
                if (isPlayerInSafeHarbor()) {
                    projectiles.remove(i);
                    continue;
                }
                double dist = Math.hypot(pr.getX() - player.getX(), pr.getY() - player.getY());
                if (dist < pr.getRadius() + player.getRadius() - 3.0) {
                    applyDamageToPlayer(pr.getDamage());
                    projectiles.remove(i);
                    removed = true;
                }
            }

            if (!removed) {
                i++;
            }
        }
    }

    private boolean hitsAnyWallOrClosedGate(double px, double py) {
        for (int i = 0; i < walls.size(); i++) {
            if (walls.get(i).containsPoint(px, py)) {
                return true;
            }
        }
        for (int i = 0; i < gates.size(); i++) {
            SectorGate g = gates.get(i);
            if (!g.isOpened() && px >= g.getX() && px <= g.getX() + g.getWidth() &&
                py >= g.getY() && py <= g.getY() + g.getHeight()) {
                return true;
            }
        }
        return false;
    }

    void handleEnemyDefeated(PirateEnemy en) {
        handleEnemyDefeated(en, Math.random());
    }

    void handleEnemyDefeated(PirateEnemy en, double logPoseRoll) {
        enemiesDefeatedCount++;
        if (en.getBerryDrop() > 0) {
            lootDrops.add(new LootDrop("BERRY", en.getX() - 12, en.getY() + 8, en.getBerryDrop()));
        }
        boolean logPoseDropped = en.isBoss()
                || shouldDropLogPose(logPoseRoll)
                || shouldGuaranteeLogPose(en);
        if (logPoseDropped) {
            lootDrops.add(new LootDrop("LOG_POSE", en.getX() + 14, en.getY() + 8, 1));
        }
        if (en.isBoss()) {
            imuDefeated = true;
            lootDrops.add(new LootDrop("ESCAPE_KEY", en.getX() + 14, en.getY() + 8, 1));
            pushEvent("IMU DEFEATED! GET TO THE SHIP!", "#fbbf24", en.getX(), en.getY() - 30);
            chests.add(new RarityChest(en.getX() - 60, en.getY() + 40, 4));
            chests.add(new RarityChest(en.getX() + 60, en.getY() + 40, 4));
            return;
        }

        if (en.getGoldKeyDrop() > 0) {
            lootDrops.add(new LootDrop("GOLD_KEY", en.getX() + 14, en.getY() + 8, 1));
        } else if (en.isImuKeycardCarrier()) {
            lootDrops.add(new LootDrop("IMU_KEYCARD", en.getX() + 14, en.getY() + 8, 1));
        } else {
            pendingEnemyRespawns.add(new PendingEnemyRespawn(en, ENEMY_RESPAWN_SECONDS));
        }
        String rewardMsg = (en.getBerryDrop() > 0 ? "BERRIES DROPPED" : "")
                + (logPoseDropped ? "  LOG POSE DROPPED" : "")
                + (en.getGoldKeyDrop() > 0 ? "  GOLD KEY DROPPED!" : "")
                + (en.isImuKeycardCarrier() ? "  IMU KEYCARD DROPPED!" : "");
        if (!rewardMsg.isEmpty()) {
            pushEvent(rewardMsg, "#34d399", en.getX(), en.getY() - 20);
        }
    }

    private boolean shouldGuaranteeLogPose(PirateEnemy defeatedEnemy) {
        int requiredPoses = DIFFICULTY_GATE_REQ[difficultyIndex] + 2;
        SectorGate nextGreenGate = null;
        for (SectorGate gate : gates) {
            if (gate.getRequiredGreenKeys() > 0 && !gate.isOpened()
                    && gate.getX() > defeatedEnemy.getX()) {
                nextGreenGate = gate;
                break;
            }
        }
        if (nextGreenGate != null) {
            requiredPoses = nextGreenGate.getRequiredGreenKeys();
        } else if (defeatedEnemy.getY() > 2500.0) {
            return false;
        }

        int missingPoses = requiredPoses - player.getGreenLogPoses();
        if (missingPoses <= 0) {
            return false;
        }

        double sectionStart = 260.0;
        double[] sectionEnds = {1740.0, 2690.0, 3590.0, 4540.0, 5360.0};
        for (double sectionEnd : sectionEnds) {
            if (defeatedEnemy.getX() < sectionEnd) {
                int remainingEnemies = 0;
                for (PirateEnemy enemy : enemies) {
                    if (enemy != defeatedEnemy && enemy.isAlive()
                            && !enemy.isBoss()
                            && enemy.getGoldKeyDrop() == 0
                            && enemy.getX() >= sectionStart
                            && enemy.getX() < sectionEnd) {
                        remainingEnemies++;
                    }
                }
                return remainingEnemies < missingPoses;
            }
            sectionStart = sectionEnd;
        }
        return false;
    }

    static boolean shouldDropLogPose(double roll) {
        return roll >= 0.0 && roll < LOG_POSE_DROP_CHANCE;
    }

    private void updateLasers(double dt) {
        int i = 0;
        while (i < lasers.size()) {
            SkyLaser lz = lasers.get(i);
            lz.update(dt);
            if (lz.isFinishedOneShot()) {
                lasers.remove(i);
                continue;
            }
            if (lz.isFiring()) {
                double dist = lz.distanceToPoint(player.getX(), player.getY());
                if (dist < player.getRadius() + lz.getWidth() * 0.42) {
                    applyDamageToPlayer(lz.getDamage());
                }
            }
            i++;
        }
    }

    private boolean isPlayerInSafeHarbor() {
        return player.getX() >= 310.0 && player.getX() <= 790.0
                && player.getY() >= 1000.0 && player.getY() <= 1600.0;
    }

    private void applyDamageToPlayer(int damage) {
        if (!isPlayerInSafeHarbor() && player.takeDamage(damage)) {
            pushEvent("-" + damage + " HP", "#f87171", player.getX(), player.getY() - 26);
            if (player.getHp() <= 0.0) {
                gameMode = "DEFEAT";
                pushEvent("DEFEATED ON THE GRAND LINE!", "#ef4444", player.getX(), player.getY() - 40);
            }
        }
    }

    public String getCurrentZoneSummary() {
        String rawCode;
        if (GOLD_KEY_REALM.equals(currentRealm)) {
            rawCode = "REALM:GOLD KEY ARENA";
        } else if (IMU_REALM.equals(currentRealm)) {
            rawCode = "REALM:THE EMPTY THRONE";
        } else if (player.getX() < 850.0) {
            rawCode = "ZONE 0:SAFE HARBOR DOCK";
        } else if (player.getX() < 1755.0) {
            rawCode = "ZONE 1:EAST BLUE CORAL SHORES";
        } else if (player.getX() < 2690.0) {
            rawCode = "ZONE 2:WINDWARD KEYS";
        } else if (player.getX() < 3590.0) {
            rawCode = "ZONE 3:PONEGLYPH JUNGLE";
        } else if (player.getX() < 4540.0) {
            rawCode = "ZONE 4:CINDER COAST";
        } else if (player.getX() < 5360.0) {
            rawCode = "ZONE 5:RED-LINE CLIFFS";
        } else {
            rawCode = "ZONE 5:RED-LINE CLIFFS";
        }
        int sep = rawCode.indexOf(":");
        if (sep != -1) {
            return rawCode.substring(0, sep) + " - " + rawCode.substring(sep + 1);
        }
        return rawCode;
    }

    private void pushEvent(String text, String color, double x, double y) {
        recentEvents.add("{\"id\":" + (nextEventId++) + ",\"text\":\"" + escapeJson(text)
                + "\",\"color\":\"" + color + "\",\"x\":" + Math.round(x) + ",\"y\":" + Math.round(y) + "}");
        if (recentEvents.size() > 14) {
            recentEvents.remove(0);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public synchronized String toJsonSnapshot() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("{");
        sb.append("\"mode\":\"").append(gameMode).append("\",");
        sb.append("\"difficulty\":\"").append(DIFFICULTY_NAMES[difficultyIndex]).append("\",");
        sb.append("\"difficultyIndex\":").append(difficultyIndex).append(",");
        sb.append("\"realm\":\"").append(currentRealm).append("\",");
        sb.append("\"gateReq\":").append(DIFFICULTY_GATE_REQ[difficultyIndex]).append(",");
        sb.append("\"elapsedTime\":").append(String.format(java.util.Locale.US, "%.2f", elapsedTime)).append(",");
        sb.append("\"zoneLabel\":\"").append(escapeJson(getCurrentZoneSummary())).append("\",");
        sb.append("\"imuDefeated\":").append(imuDefeated).append(",");
        sb.append("\"secretVaultUnlocked\":").append(secretVaultUnlocked).append(",");
        sb.append("\"gear5Unlocked\":").append(gear5Unlocked).append(",");

        sb.append("\"metrics\":{");
        sb.append("\"kills\":").append(enemiesDefeatedCount).append(",");
        sb.append("\"chestsOpened\":").append(chestsOpenedCount);
        sb.append("},");

        sb.append("\"player\":{");
        sb.append("\"callsign\":\"").append(escapeJson(player.getCaptainCallsign())).append("\",");
        sb.append("\"x\":").append(String.format(java.util.Locale.US, "%.1f", player.getX())).append(",");
        sb.append("\"y\":").append(String.format(java.util.Locale.US, "%.1f", player.getY())).append(",");
        sb.append("\"radius\":").append(player.getRadius()).append(",");
        sb.append("\"hp\":").append(Math.ceil(player.getHp())).append(",");
        sb.append("\"maxHp\":").append(Math.round(player.getMaxHp())).append(",");
        sb.append("\"energy\":").append(Math.floor(player.getEnergy())).append(",");
        sb.append("\"maxEnergy\":").append(Math.round(player.getMaxEnergy())).append(",");
        sb.append("\"berries\":").append(player.getBerries()).append(",");
        sb.append("\"greenKeys\":").append(player.getGreenLogPoses()).append(",");
        sb.append("\"goldKeys\":").append(player.getGoldKeycards()).append(",");
        sb.append("\"imuKeycards\":").append(player.getImuKeycards()).append(",");
        sb.append("\"escapeKey\":").append(player.hasEscapeKey()).append(",");
        sb.append("\"gear5Active\":").append(player.hasGear5()).append(",");
        sb.append("\"aimAngle\":").append(String.format(java.util.Locale.US, "%.3f", player.getAimAngle())).append(",");
        sb.append("\"walkBob\":").append(String.format(java.util.Locale.US, "%.2f", player.getWalkBob())).append(",");
        sb.append("\"dashing\":").append(player.isDashing()).append(",");
        sb.append("\"equippedIndex\":").append(player.getEquippedWeaponIndex()).append(",");
        sb.append("\"weapons\":[");
        List<Weapon> weps = player.getWeapons();
        for (int i = 0; i < weps.size(); i++) {
            Weapon w = weps.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":\"").append(w.getId()).append("\",");
            sb.append("\"name\":\"").append(escapeJson(w.getName())).append("\",");
            sb.append("\"gimkitRef\":\"").append(escapeJson(w.getGimkitRef())).append("\",");
            sb.append("\"icon\":\"").append(w.getIcon()).append("\",");
            sb.append("\"cost\":").append(w.getUnlockCost()).append(",");
            sb.append("\"energyCost\":").append(Weapon.ENERGY_COST_PER_SHOT).append(",");
            sb.append("\"unlocked\":").append(w.isUnlocked()).append(",");
            sb.append("\"rarityIndex\":").append(w.getRarityIndex()).append(",");
            sb.append("\"rarityName\":\"").append(w.getRarityName()).append("\",");
            sb.append("\"rarityColor\":\"").append(w.getRarityColor()).append("\",");
            sb.append("\"upgradeCost\":").append(w.getNextUpgradeCost()).append(",");
            sb.append("\"maxRarity\":").append(w.isMaxRarity()).append(",");
            sb.append("\"damage\":").append(w.getScaledDamage()).append(",");
            sb.append("\"ammo\":").append(w.getCurrentAmmo()).append(",");
            sb.append("\"clipSize\":").append(w.getClipSize()).append(",");
            sb.append("\"reloading\":").append(w.isReloading()).append(",");
            sb.append("\"desc\":\"").append(escapeJson(w.getDescription())).append("\"");
            sb.append("}");
        }
        sb.append("]},");

        sb.append("\"walls\":[");
        for (int i = 0; i < walls.size(); i++) {
            WorldObstacle w = walls.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"x\":").append(w.getX()).append(",\"y\":").append(w.getY())
              .append(",\"w\":").append(w.getWidth()).append(",\"h\":").append(w.getHeight())
              .append(",\"type\":\"").append(w.getMaterialType()).append("\"}");
        }
        sb.append("],");

        sb.append("\"gates\":[");
        for (int i = 0; i < gates.size(); i++) {
            SectorGate g = gates.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"").append(g.getId()).append("\",\"name\":\"").append(escapeJson(g.getName()))
              .append("\",\"x\":").append(g.getX()).append(",\"y\":").append(g.getY())
              .append(",\"w\":").append(g.getWidth()).append(",\"h\":").append(g.getHeight())
              .append(",\"reqGreen\":").append(g.getRequiredGreenKeys())
              .append(",\"reqGold\":").append(g.getRequiredGoldKeys())
              .append(",\"reqSpecial\":").append(g.getRequiredSpecialKeys())
              .append(",\"opened\":").append(g.isOpened())
              .append(",\"color\":\"").append(g.getHexColor()).append("\"}");
        }
        sb.append("],");

        sb.append("\"portals\":[");
        boolean firstPortal = true;
        for (PortalRoute portal : portals) {
            if (!portal.realm.equals(currentRealm)) continue;
            if (!firstPortal) sb.append(",");
            firstPortal = false;
            sb.append("{\"id\":\"").append(portal.id).append("\",\"name\":\"")
              .append(escapeJson(portal.name)).append("\",\"x\":").append(portal.x)
              .append(",\"y\":").append(portal.y).append("}");
        }
        sb.append("],");

        sb.append("\"chests\":[");
        for (int i = 0; i < chests.size(); i++) {
            RarityChest c = chests.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"x\":").append(Math.round(c.getX())).append(",\"y\":").append(Math.round(c.getY()))
              .append(",\"rarityIndex\":").append(c.getRarityIndex())
              .append(",\"opened\":").append(c.isOpened())
              .append(",\"bob\":").append(String.format(java.util.Locale.US, "%.2f", c.getBobPhase())).append("}");
        }
        sb.append("],");

        sb.append("\"lootDrops\":[");
        for (int i = 0; i < lootDrops.size(); i++) {
            LootDrop drop = lootDrops.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"type\":\"").append(drop.getType()).append("\",\"x\":")
              .append(String.format(java.util.Locale.US, "%.1f", drop.getX()))
              .append(",\"y\":").append(String.format(java.util.Locale.US, "%.1f", drop.getY()))
              .append(",\"amount\":").append(drop.getAmount())
              .append(",\"bob\":").append(String.format(java.util.Locale.US, "%.2f", drop.getBobPhase())).append("}");
        }
        sb.append("],");

        sb.append("\"enemies\":[");
        for (int i = 0; i < enemies.size(); i++) {
            PirateEnemy e = enemies.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(e.getId()).append(",\"type\":\"").append(e.getType())
              .append("\",\"name\":\"").append(escapeJson(e.getName()))
              .append("\",\"x\":").append(String.format(java.util.Locale.US, "%.1f", e.getX()))
              .append(",\"y\":").append(String.format(java.util.Locale.US, "%.1f", e.getY()))
              .append(",\"radius\":").append(e.getRadius())
              .append(",\"speed\":").append(String.format(java.util.Locale.US, "%.1f", e.getSpeed()))
              .append(",\"attackRange\":").append(String.format(java.util.Locale.US, "%.1f", e.getAttackRange()))
              .append(",\"hp\":").append(e.getHp()).append(",\"maxHp\":").append(e.getMaxHp())
              .append(",\"color\":\"").append(e.getPrimaryColor()).append("\",\"accent\":\"").append(e.getAccentColor())
              .append("\",\"boss\":").append(e.isBoss())
              .append(",\"hitFlash\":").append(e.getHitFlashTimer() > 0)
              .append(",\"aimAngle\":").append(String.format(java.util.Locale.US, "%.2f", e.getAimAngle())).append("}");
        }
        sb.append("],");

        sb.append("\"projectiles\":[");
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile p = projectiles.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(p.getId())
              .append(",\"x\":").append(String.format(java.util.Locale.US, "%.1f", p.getX()))
              .append(",\"y\":").append(String.format(java.util.Locale.US, "%.1f", p.getY()))
              .append(",\"vx\":").append(String.format(java.util.Locale.US, "%.1f", p.getVx()))
              .append(",\"vy\":").append(String.format(java.util.Locale.US, "%.1f", p.getVy()))
              .append(",\"radius\":").append(p.getRadius())
              .append(",\"color\":\"").append(p.getColor())
              .append("\",\"rarityColor\":\"").append(p.getRarityColor()).append("\"}");
        }
        sb.append("],");

        sb.append("\"lasers\":[");
        for (int i = 0; i < lasers.size(); i++) {
            SkyLaser l = lasers.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"x1\":").append(Math.round(l.getX1())).append(",\"y1\":").append(Math.round(l.getY1()))
              .append(",\"x2\":").append(Math.round(l.getX2())).append(",\"y2\":").append(Math.round(l.getY2()))
              .append(",\"width\":").append(l.getWidth())
              .append(",\"warning\":").append(l.isWarning())
              .append(",\"firing\":").append(l.isFiring()).append("}");
        }
        sb.append("],");

        sb.append("\"events\":[");
        for (int i = 0; i < recentEvents.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(recentEvents.get(i));
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public Player getPlayer() { return player; }
    public String getGameMode() { return gameMode; }
    public boolean isImuDefeated() { return imuDefeated; }
    public String getCurrentRealm() { return currentRealm; }
    List<PirateEnemy> getEnemies() { return enemies; }
    List<WorldObstacle> getWalls() { return walls; }
    public List<RarityChest> getChests() { return chests; }
    public List<SectorGate> getGates() { return gates; }
    List<LootDrop> getLootDrops() { return lootDrops; }
    List<Projectile> getProjectiles() { return projectiles; }
    List<SkyLaser> getLasers() { return lasers; }
}
