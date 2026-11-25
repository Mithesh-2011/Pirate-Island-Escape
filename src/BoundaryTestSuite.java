public class BoundaryTestSuite {

    public static void main(String[] args) {
        runAllTests();
    }

    public static void runAllTests() {
        System.out.println("=== Running Boundary & Reliability Tests ===");

        SkyLaser repeatingLaser = new SkyLaser(0, 0, 0, 100, 3.6,
                0.4, 1.3, 0.85, 26, false, 28);
        repeatingLaser.update(0.89);
        assertCondition(repeatingLaser.isWarning() && !repeatingLaser.isFiring(),
                "Section laser warning precedes its firing window");
        repeatingLaser.update(0.02);
        assertCondition(repeatingLaser.isFiring(),
                "Section laser enters its firing window on schedule");
        repeatingLaser.update(2.4);
        assertCondition(repeatingLaser.isWarning() && !repeatingLaser.isFiring(),
                "Section laser returns to warning after firing");
        repeatingLaser.update(7.2);
        assertCondition(repeatingLaser.isWarning() && !repeatingLaser.isFiring(),
                "Section laser preserves its cycle after multiple missed periods");
        GameEngine sectionFiveLaserEngine = new GameEngine();
        sectionFiveLaserEngine.resetGame("Normal", "Section Five Laser Test");
        sectionFiveLaserEngine.getPlayer().setPosition(4660, 1300);
        assertCondition(sectionFiveLaserEngine.getLasers().isEmpty(),
                "The fifth section starts without environmental laser hazards");
        for (int i = 0; i < 300; i++) {
            sectionFiveLaserEngine.tick(1.0 / 60.0);
        }
        assertCondition(sectionFiveLaserEngine.getLasers().isEmpty(),
                "The fifth section remains free of environmental lasers");

        GameEngine imuLaserEngine = new GameEngine();
        imuLaserEngine.resetGame("Normal", "Boss Laser Test");
        imuLaserEngine.getPlayer().setPosition(5100, 1300);
        imuLaserEngine.getPlayer().addImuKeycard();
        assertCondition("PORTAL_TRAVELLED".equals(imuLaserEngine.executePlayerAction("INTERACT", 0)),
                "Boss laser test enters the Empty Throne realm");
        for (int i = 0; i < 240; i++) {
            imuLaserEngine.tick(1.0 / 60.0);
        }
        assertCondition(!imuLaserEngine.getLasers().isEmpty(),
                "IMU continues to summon one-shot laser attacks");

        Player testPlayer = new Player("StrawHatLuffyPirateKing:AdminHack", 500, 500);
        assertCondition(testPlayer.getCaptainCallsign().length() <= 18, "Callsign truncated via length() & substring()");
        assertCondition(testPlayer.getCaptainCallsign().indexOf(":") == -1, "Disallowed colon stripped via indexOf()");

        Player luffyAlias = new Player("Luffy", 500, 500);
        assertCondition("Monkey D. Luffy".equals(luffyAlias.getCaptainCallsign()), "Alias normalized via equals()");
        assertCondition(luffyAlias.outranksLexicographically("Zoro"), "Rank compared via compareTo()");
        assertCondition(luffyAlias.getBerries() == 0, "Player starts without berries");
        Player energyPlayer = new Player("Energy Test", 0, 0);
        assertCondition(energyPlayer.tryDash(1, 0), "Energy regeneration test consumes dash energy");
        energyPlayer.regeneratePassiveEnergy(1.0);
        assertCondition(energyPlayer.getEnergy() == 76.0, "Passive energy regenerates at one energy per second");
        GameEngine weaponEnergyEngine = new GameEngine();
        weaponEnergyEngine.resetGame("Normal", "Weapon Energy Test");
        weaponEnergyEngine.getPlayer().setPosition(900, 1300);
        double energyBeforeShot = weaponEnergyEngine.getPlayer().getEnergy();
        assertCondition("FIRED".equals(weaponEnergyEngine.executePlayerAction("FIRE", 0))
                && weaponEnergyEngine.getPlayer().getEnergy()
                        == energyBeforeShot - Weapon.ENERGY_COST_PER_SHOT,
                "Firing a weapon consumes energy");
        GameEngine reloadingWeaponEngine = new GameEngine();
        reloadingWeaponEngine.resetGame("Normal", "Reload Fire Test");
        reloadingWeaponEngine.getPlayer().setPosition(1000, 1300);
        Weapon reloadingWeapon = reloadingWeaponEngine.getPlayer().getEquippedWeapon();
        reloadingWeapon.consumeShot();
        reloadingWeapon.startReload();
        int reloadAmmo = reloadingWeapon.getCurrentAmmo();
        assertCondition("RELOADING".equals(reloadingWeaponEngine.executePlayerAction("FIRE", 0))
                        && reloadingWeaponEngine.getProjectiles().isEmpty()
                        && reloadingWeapon.getCurrentAmmo() == reloadAmmo,
                "Firing during reload is rejected without consuming ammo or creating a shot");
        assertCondition(weaponEnergyEngine.toJsonSnapshot().contains("\"energyCost\":1.0"),
                "Weapon energy costs are included in the game snapshot");
        String movementSnapshot = weaponEnergyEngine.toJsonSnapshot();
        assertCondition(movementSnapshot.contains("\"speed\":")
                        && movementSnapshot.contains("\"attackRange\":")
                        && movementSnapshot.contains("\"projectiles\":[{\"id\":")
                        && movementSnapshot.contains("\"vx\":")
                        && movementSnapshot.contains("\"vy\":"),
                "Snapshots include the movement data used by the browser renderer");
        Projectile browserMovedProjectile = weaponEnergyEngine.getProjectiles().get(0);
        double projectileStartX = browserMovedProjectile.getX();
        weaponEnergyEngine.tick(0.1);
        assertCondition(browserMovedProjectile.getX() == projectileStartX,
                "The Java tick leaves projectile movement to the browser");
        GameEngine browserPlayerMovementEngine = new GameEngine();
        browserPlayerMovementEngine.resetGame("Normal", "Browser Movement Test");
        double playerStartX = browserPlayerMovementEngine.getPlayer().getX();
        browserPlayerMovementEngine.updateInputState(false, false, false, true, false, 0);
        browserPlayerMovementEngine.tick(0.1);
        assertCondition(browserPlayerMovementEngine.getPlayer().getX() == playerStartX,
                "The Java tick leaves player movement to the browser");
        browserPlayerMovementEngine.updateClientMovement(playerStartX + 6, 1300,
                java.util.Collections.emptyMap(), java.util.Collections.emptyMap());
        assertCondition(browserPlayerMovementEngine.getPlayer().getX() == playerStartX + 6,
                "Client player positions synchronize to Java for collision checks");
        GameEngine safeHarborWeaponEngine = new GameEngine();
        safeHarborWeaponEngine.resetGame("Normal", "Safe Harbor Weapon Test");
        Weapon harborWeapon = safeHarborWeaponEngine.getPlayer().getEquippedWeapon();
        int harborAmmo = harborWeapon.getCurrentAmmo();
        double harborEnergy = safeHarborWeaponEngine.getPlayer().getEnergy();
        assertCondition("SAFE_HARBOR".equals(safeHarborWeaponEngine.executePlayerAction("FIRE", 0))
                && harborWeapon.getCurrentAmmo() == harborAmmo
                && safeHarborWeaponEngine.getPlayer().getEnergy() == harborEnergy,
                "Weapons cannot be fired inside the safe harbor");
        safeHarborWeaponEngine.updateInputState(false, false, false, false, true, 0);
        safeHarborWeaponEngine.tick(1.0);
        assertCondition(harborWeapon.getCurrentAmmo() == harborAmmo,
                "Holding fire inside the safe harbor does not consume ammunition");
        safeHarborWeaponEngine.getPlayer().setPosition(310, 1000);
        assertCondition("SAFE_HARBOR".equals(safeHarborWeaponEngine.executePlayerAction("FIRE", 0)),
                "The safe harbor covers the upper-left corner of the brown base");
        safeHarborWeaponEngine.getPlayer().setPosition(790, 1600);
        assertCondition("SAFE_HARBOR".equals(safeHarborWeaponEngine.executePlayerAction("FIRE", 0)),
                "The safe harbor covers the lower-right corner of the brown base");
        safeHarborWeaponEngine.getPlayer().setPosition(309, 1000);
        assertCondition("FIRED".equals(safeHarborWeaponEngine.executePlayerAction("FIRE", 0)),
                "The safe harbor boundary ends outside the brown base");
        GameEngine pauseEngine = new GameEngine();
        pauseEngine.resetGame("Normal", "Pause Test");
        String pausedElapsedTime = pauseEngine.toJsonSnapshot().split("\"elapsedTime\":")[1].split(",")[0];
        assertCondition("PAUSED".equals(pauseEngine.executePlayerAction("PAUSE", 0)),
                "Pause action changes the game mode");
        pauseEngine.tick(1.0);
        assertCondition("PAUSED".equals(pauseEngine.getGameMode())
                && pausedElapsedTime.equals(pauseEngine.toJsonSnapshot()
                        .split("\"elapsedTime\":")[1].split(",")[0]),
                "Paused games stop advancing");
        assertCondition("RESUMED".equals(pauseEngine.executePlayerAction("PAUSE", 0)),
                "Pause action resumes the game");
        GameEngine replayEngine = new GameEngine();
        replayEngine.resetGame("Normal", "Replay Test");
        PirateEnemy replayBoss = replayEngine.getEnemies().stream()
                .filter(PirateEnemy::isBoss).findFirst().orElseThrow();
        replayBoss.takeDamage(replayBoss.getHp());
        replayEngine.handleEnemyDefeated(replayBoss, 0.99);
        assertCondition(replayEngine.getLootDrops().stream()
                        .anyMatch(drop -> "ESCAPE_KEY".equals(drop.getType()))
                        && replayEngine.toJsonSnapshot().contains("\"escapeKey\":false")
                        && replayEngine.toJsonSnapshot().contains("IMU DEFEATED! GET TO THE SHIP!"),
                "Defeating IMU drops a physical Escape Key and signals the return to ship");
        replayEngine.getPlayer().setPosition(450, 1300);
        assertCondition("NO_ESCAPE_KEY".equals(replayEngine.executePlayerAction("INTERACT", 0)),
                "The ship cannot be activated without collecting the Escape Key");
        LootDrop escapeKey = replayEngine.getLootDrops().stream()
                .filter(drop -> "ESCAPE_KEY".equals(drop.getType())).findFirst().orElseThrow();
        replayEngine.getPlayer().setPosition(escapeKey.getX(), escapeKey.getY());
        replayEngine.tick(1.0 / 60.0);
        assertCondition(replayEngine.getPlayer().hasEscapeKey()
                        && replayEngine.toJsonSnapshot().contains("\"escapeKey\":true"),
                "Collecting IMU's Escape Key enables the ship");
        replayEngine.getPlayer().setPosition(450, 1300);
        assertCondition("VICTORY".equals(replayEngine.executePlayerAction("INTERACT", 0)),
                "Pressing E at the ship with the Escape Key wins the run");
        replayEngine.resetGame("Hard", "Replay Test");
        assertCondition("PLAYING".equals(replayEngine.getGameMode())
                && replayEngine.getPlayer().getHp() == replayEngine.getPlayer().getMaxHp()
                && replayEngine.getPlayer().getBerries() == 0
                && replayEngine.toJsonSnapshot().contains("\"elapsedTime\":0.00"),
                "Starting after a victory creates a fresh playable run");
        assertCondition(GameEngine.shouldDropLogPose(0.0)
                && GameEngine.shouldDropLogPose(0.4999)
                && !GameEngine.shouldDropLogPose(0.50)
                && !GameEngine.shouldDropLogPose(1.0),
                "Log Pose drop chance uses the exact 50 percent boundary");
        GameEngine sectionEngine = new GameEngine();
        sectionEngine.resetGame("Normal", "Section Count Test");
        assertCondition(hasExactSectionEnemyCounts(sectionEngine, 2),
                "All five sections have the expected enemy counts");
        double[][] obstacleSections = {
            {840, 1740}, {1778, 2690}, {2728, 3590}, {3628, 4540}, {4578, 5360}
        };
        boolean evenlySpacedObstacles = true;
        for (double[] sectionBounds : obstacleSections) {
            long obstacleCount = sectionEngine.getWalls().stream()
                    .filter(wall -> "crate".equals(wall.getMaterialType())
                            || "tree".equals(wall.getMaterialType()))
                    .filter(wall -> wall.getX() >= sectionBounds[0]
                            && wall.getX() + wall.getWidth() <= sectionBounds[1])
                    .count();
            long treeCount = sectionEngine.getWalls().stream()
                    .filter(wall -> "tree".equals(wall.getMaterialType()))
                    .filter(wall -> wall.getX() >= sectionBounds[0]
                            && wall.getX() + wall.getWidth() <= sectionBounds[1])
                    .count();
            evenlySpacedObstacles &= obstacleCount == 20 && treeCount == 10;
        }
        assertCondition(evenlySpacedObstacles,
                "All five sections have twenty alternating, staggered crates and trees");
        boolean entitiesAvoidObstacles = sectionEngine.getEnemies().stream()
                .allMatch(enemy -> !overlapsSectionObstacle(sectionEngine, enemy.getX(), enemy.getY(),
                        enemy.getRadius()))
                && sectionEngine.getChests().stream()
                .allMatch(chest -> !overlapsSectionObstacle(sectionEngine, chest.getX(), chest.getY(), 36));
        double[][] portalLocations = {
            {2340, 1300}, {7100, 1500}, {7100, 1800},
            {5100, 1300}, {9300, 1800}, {9600, 1900}
        };
        for (double[] portal : portalLocations) {
            entitiesAvoidObstacles &= !overlapsSectionObstacle(sectionEngine, portal[0], portal[1], 48);
        }
        assertCondition(entitiesAvoidObstacles,
                "Enemy, portal, and chest spawn locations stay clear of obstacles");
        WorldObstacle treeObstacle = sectionEngine.getWalls().stream()
                .filter(wall -> "tree".equals(wall.getMaterialType())).findFirst().orElseThrow();
        double treeCenterX = treeObstacle.getX() + treeObstacle.getWidth() / 2.0;
        double treeCenterY = treeObstacle.getY() + treeObstacle.getHeight() / 2.0;
        sectionEngine.getPlayer().setPosition(treeCenterX, treeCenterY);
        sectionEngine.updateClientMovement(treeCenterX + 1, treeCenterY,
                java.util.Collections.emptyMap(), java.util.Collections.emptyMap());
        sectionEngine.tick(1.0 / 60.0);
        assertCondition(treeObstacle.containsPoint(treeCenterX, treeCenterY)
                        && !treeObstacle.containsPoint(sectionEngine.getPlayer().getX(),
                                sectionEngine.getPlayer().getY()),
                "Tree trunks have solid player collision hitboxes");
        WorldObstacle boxObstacle = sectionEngine.getWalls().stream()
                .filter(wall -> "crate".equals(wall.getMaterialType())).findFirst().orElseThrow();
        double boxCenterX = boxObstacle.getX() + boxObstacle.getWidth() / 2.0;
        double boxCenterY = boxObstacle.getY() + boxObstacle.getHeight() / 2.0;
        double[] boxResolved = boxObstacle.resolveCircleCollision(boxCenterX, boxCenterY, 20.0);
        assertCondition(!boxObstacle.containsPoint(boxResolved[0], boxResolved[1]),
                "Overlapping obstacle resolution moves entities fully out of boxes");
        assertCondition(hasChestInEveryMapSection(sectionEngine),
                "Chests spawn in the harbor, all five sections, and throne area");
        PirateEnemy[] firstSectionEnemies = sectionEngine.getEnemies().stream()
                .filter(enemy -> !enemy.isBoss() && enemy.getX() < 1740)
                .limit(3).toArray(PirateEnemy[]::new);
        for (PirateEnemy enemy : firstSectionEnemies) {
            enemy.takeDamage(enemy.getHp());
            sectionEngine.handleEnemyDefeated(enemy, 0.99);
        }
        assertCondition(sectionEngine.getLootDrops().stream()
                .anyMatch(drop -> "LOG_POSE".equals(drop.getType())),
                "Last enemies guarantee enough Log Poses to open their section gate");

        GameEngine keyEngine = new GameEngine();
        keyEngine.resetGame("Normal", "Gold Key Test");
        assertCondition(keyEngine.getEnemies().stream()
                .filter(enemy -> !enemy.isBoss() && !"gold_guardian".equals(enemy.getType()))
                .allMatch(enemy -> enemy.getGoldKeyDrop() == 0),
                "Only Gold Key Guardians carry Gold Keys");
        PirateEnemy ordinaryEnemy = keyEngine.getEnemies().stream()
                .filter(enemy -> !enemy.isBoss() && !"gold_guardian".equals(enemy.getType()))
                .findFirst().orElseThrow();
        ordinaryEnemy.takeDamage(ordinaryEnemy.getHp());
        keyEngine.handleEnemyDefeated(ordinaryEnemy, 0.99);
        assertCondition(keyEngine.getPlayer().getGoldKeycards() == 0
                && keyEngine.getLootDrops().stream().noneMatch(drop -> "GOLD_KEY".equals(drop.getType())),
                "Regular enemies do not award Gold Keys");
        PirateEnemy[] guardians = keyEngine.getEnemies().stream()
                .filter(enemy -> "gold_guardian".equals(enemy.getType())).toArray(PirateEnemy[]::new);
        assertCondition(guardians.length == 3
                && java.util.Arrays.stream(guardians).allMatch(enemy ->
                        enemy.getX() >= 6500 && enemy.getX() <= 7800
                                && enemy.getGoldKeyDrop() == 1)
                && java.util.Arrays.stream(guardians).allMatch(enemy ->
                        enemy.getMaxHp() > ordinaryEnemy.getMaxHp()),
                "Three harder, one-time Gold Key Guardians are isolated in their realm");
        keyEngine.getPlayer().setPosition(2340, 1300);
        assertCondition("PORTAL_TRAVELLED".equals(keyEngine.executePlayerAction("INTERACT", 0))
                && "GOLD_KEY".equals(keyEngine.getCurrentRealm())
                && keyEngine.toJsonSnapshot().contains("\"id\":\"gold_key_exit\"")
                && keyEngine.getEnemies().stream().filter(enemy ->
                        "gold_guardian".equals(enemy.getType())).allMatch(enemy -> enemy.getX() > 6500),
                "The section 2 portal leads to an isolated Gold Key realm");
        for (PirateEnemy guardian : guardians) {
            guardian.takeDamage(guardian.getHp());
            keyEngine.handleEnemyDefeated(guardian, 0.99);
        }
        assertCondition(keyEngine.getPlayer().getGoldKeycards() == 0
                && keyEngine.getLootDrops().stream()
                        .filter(drop -> "GOLD_KEY".equals(drop.getType())).count() == 3,
                "Each Gold Key Guardian drops one physical Gold Key");
        assertCondition(keyEngine.toJsonSnapshot().contains("\"type\":\"GOLD_KEY\""),
                "Gold Key drops are represented in the game snapshot");
        keyEngine.updateInputState(false, false, false, false, false, 0);
        for (LootDrop drop : keyEngine.getLootDrops().toArray(new LootDrop[0])) {
            if ("GOLD_KEY".equals(drop.getType())) {
                keyEngine.getPlayer().setPosition(drop.getX(), drop.getY());
                keyEngine.tick(1.0 / 60.0);
            }
        }
        assertCondition(keyEngine.getPlayer().getGoldKeycards() == 3,
                "Collecting the three realm drops grants the Gold Keys");
        keyEngine.tick(GameEngine.ENEMY_RESPAWN_SECONDS);
        assertCondition(keyEngine.getEnemies().stream()
                .noneMatch(enemy -> "gold_guardian".equals(enemy.getType()) && enemy.isAlive()),
                "Gold Key Guardians never respawn");
        keyEngine.getPlayer().setPosition(7100, 1800);
        assertCondition("PORTAL_TRAVELLED".equals(keyEngine.executePlayerAction("INTERACT", 0))
                && "MAIN".equals(keyEngine.getCurrentRealm()),
                "The Gold Key portal provides a return route to section 2");
        GameEngine lockedVaultEngine = new GameEngine();
        lockedVaultEngine.resetGame("Normal", "Locked Vault Test");
        lockedVaultEngine.getChests().forEach(chest -> chest.markOpened(40.0));
        lockedVaultEngine.getPlayer().setPosition(3120, 540);
        assertCondition("GATE_LOCKED".equals(lockedVaultEngine.executePlayerAction("INTERACT", 0)),
                "The Secret Vault cannot be opened without Gold Keys");
        keyEngine.getChests().forEach(chest -> chest.markOpened(40.0));
        keyEngine.getPlayer().setPosition(3120, 540);
        assertCondition("GATE_UNLOCKED".equals(keyEngine.executePlayerAction("INTERACT", 0)),
                "Three Gold Keys open the Secret Vault");
        keyEngine.getPlayer().setPosition(3120, 540);
        assertCondition("SECRET_CLAIMED".equals(keyEngine.executePlayerAction("INTERACT", 0))
                && keyEngine.getPlayer().hasGear5()
                && keyEngine.getPlayer().getWeapons().size() == 5
                && keyEngine.getPlayer().getMaxHp() == 150.0
                && keyEngine.getPlayer().getDamageMultiplier() == 1.4
                && keyEngine.toJsonSnapshot().contains("\"gear5Active\":true"),
                "The Gold Key vault grants Gear 5 stats without adding a weapon");
        keyEngine.getPlayer().consumeEnergy(10.0);
        double gear5Energy = keyEngine.getPlayer().getEnergy();
        keyEngine.getPlayer().regeneratePassiveEnergy(1.0);
        assertCondition(keyEngine.getPlayer().getEnergy() == Math.min(
                        keyEngine.getPlayer().getMaxEnergy(), gear5Energy + 2.0),
                "Gear 5 regenerates two energy per second");
        keyEngine.getPlayer().applyChestRewards(0, 0, 0, true);
        assertCondition(keyEngine.getPlayer().getMaxHp() == 150.0,
                "Legendary chests do not raise Gear 5 max health above 150");
        GameEngine gearDamageEngine = new GameEngine();
        gearDamageEngine.resetGame("Normal", "Gear 5 Damage Test");
        gearDamageEngine.getPlayer().setPosition(900, 1300);
        gearDamageEngine.getPlayer().setAimAngle(0.0);
        int baseShotDamage = gearDamageEngine.getPlayer().getEquippedWeapon().getScaledDamage();
        gearDamageEngine.getPlayer().activateGear5();
        assertCondition("FIRED".equals(gearDamageEngine.executePlayerAction("FIRE", 0))
                && gearDamageEngine.getProjectiles().get(0).getDamage()
                        == (int) Math.round(baseShotDamage * 1.4),
                "Gear 5 increases actual projectile damage by 40 percent");
        GameEngine bossRewardEngine = new GameEngine();
        bossRewardEngine.resetGame("Normal", "Boss Return Reward Test");
        PirateEnemy finalBoss = bossRewardEngine.getEnemies().stream()
                .filter(PirateEnemy::isBoss).findFirst().orElseThrow();
        finalBoss.takeDamage(finalBoss.getHp());
        bossRewardEngine.handleEnemyDefeated(finalBoss, 0.99);
        assertCondition(bossRewardEngine.getLootDrops().stream()
                .anyMatch(drop -> "LOG_POSE".equals(drop.getType())),
                "The final boss guarantees a Log Pose to pay for the return trip");
        assertCondition(Weapon.UPGRADE_COSTS[0] == 23 && Weapon.UPGRADE_COSTS[3] == 140,
                "Weapon rarity upgrade prices are half their previous values");
        assertCondition(new Player("Armory Cost Test", 0, 0).getWeapons().get(1).getUnlockCost() == 15
                && new Player("Armory Cost Test", 0, 0).getWeapons().get(4).getUnlockCost() == 68,
                "Armory prices start at 15 berries and are slightly cheaper");
        assertCondition(GameEngine.ENEMY_RESPAWN_SECONDS == 180.0, "Pirates respawn after three minutes");
        Player reloadPlayer = new Player("Reload Test", 0, 0);
        assertCondition(reloadPlayer.getWeapons().stream().allMatch(weapon ->
                weapon.getReloadDuration() == 3.0), "All weapons reload in three seconds");
        Weapon reloadTestWeapon = reloadPlayer.getWeapons().get(0);
        reloadTestWeapon.consumeShot();
        reloadTestWeapon.startReload();
        reloadTestWeapon.updateTimers(2.99);
        assertCondition(reloadTestWeapon.isReloading(), "Reload stays active before three seconds");
        reloadTestWeapon.updateTimers(0.02);
        assertCondition(!reloadTestWeapon.isReloading(), "Reload completes at three seconds");
        int chestBerryTotal = 0;
        for (int rarity = 0; rarity < 5; rarity++) {
            chestBerryTotal += new RarityChest(0, 0, rarity).getBerryReward();
        }
        assertCondition(chestBerryTotal == 30 && chestBerryTotal >= 26 * 1.15,
                "Chest berries are about 15 percent higher than the previous total");
        PirateEnemy fallenPirate = new PirateEnemy(1, "test", "Test Pirate", 40, 50, 10,
                20, 1, 1, 1, 10, 8, 0, 0, "#000000", "#ffffff", false, 1.0, 1.0);
        assertCondition(fallenPirate.getBerryDrop() == 9,
                "Pirate berry drops are about 15 percent higher");
        fallenPirate.takeDamage(20);
        PirateEnemy returnedPirate = fallenPirate.respawnAs(2);
        assertCondition(returnedPirate.isAlive() && returnedPirate.getHp() == returnedPirate.getMaxHp()
                && returnedPirate.getId() == 2, "Respawned pirates return alive with fresh identity");

        GameEngine respawnEngine = new GameEngine();
        respawnEngine.resetGame("Normal", "Test Captain");
        PirateEnemy defeatedPirate = respawnEngine.getEnemies().get(0);
        int defeatedPirateId = defeatedPirate.getId();
        double defeatedPirateX = defeatedPirate.getX();
        double defeatedPirateY = defeatedPirate.getY();
        defeatedPirate.takeDamage(defeatedPirate.getHp());
        respawnEngine.handleEnemyDefeated(defeatedPirate);
        respawnEngine.tick(GameEngine.ENEMY_RESPAWN_SECONDS - 0.01);
        assertCondition(respawnEngine.getEnemies().stream().noneMatch(enemy ->
                enemy.getX() == defeatedPirateX && enemy.getY() == defeatedPirateY
                        && enemy.getId() != defeatedPirateId),
                "Defeated pirates do not respawn before three minutes");
        respawnEngine.tick(0.02);
        assertCondition(respawnEngine.getEnemies().stream().anyMatch(enemy ->
                enemy.getX() == defeatedPirateX && enemy.getY() == defeatedPirateY
                        && enemy.getId() != defeatedPirateId && enemy.isAlive()),
                "Defeated pirates respawn after three minutes");
        assertCondition(respawnEngine.getEnemies().stream()
                        .allMatch(enemy -> !overlapsSectionObstacle(respawnEngine,
                                enemy.getX(), enemy.getY(), enemy.getRadius())),
                "Respawned enemies remain clear of obstacles");

        GameEngine gateEngine = new GameEngine();
        gateEngine.resetGame("Normal", "Gate Test");
        PirateEnemy gateEnemy = gateEngine.getEnemies().get(0);
        gateEnemy.setPosition(1700, 1300);
        gateEngine.getPlayer().setPosition(2050, 1300);
        for (int i = 0; i < 120; i++) {
            gateEngine.tick(1.0 / 60.0);
        }
        assertCondition(gateEnemy.getX() == 1700,
                "The Java tick no longer simulates enemy movement");
        gateEngine.updateClientMovement(gateEngine.getPlayer().getX(), gateEngine.getPlayer().getY(),
                java.util.Map.of(gateEnemy.getId(), new double[]{1920, 1300}),
                java.util.Collections.emptyMap());
        assertCondition(gateEnemy.getX() + gateEnemy.getRadius() <=
                gateEngine.getGates().get(0).getX() + 0.01,
                "Client enemy positions are still constrained by closed gates");
        gateEngine.getGates().get(0).setOpened(true);
        gateEngine.updateClientMovement(gateEngine.getPlayer().getX(), gateEngine.getPlayer().getY(),
                java.util.Map.of(gateEnemy.getId(), new double[]{1920, 1300}),
                java.util.Collections.emptyMap());
        assertCondition(gateEnemy.getX() > gateEngine.getGates().get(0).getX()
                + gateEngine.getGates().get(0).getWidth(),
                "Enemies can cross an opened sector gate");

        GameEngine lootEngine = new GameEngine();
        lootEngine.resetGame("Normal", "Loot Test");
        PirateEnemy defeatedLootEnemy = lootEngine.getEnemies().get(0);
        lootEngine.getPlayer().setPosition(defeatedLootEnemy.getX(), defeatedLootEnemy.getY());
        defeatedLootEnemy.takeDamage(defeatedLootEnemy.getHp());
        lootEngine.handleEnemyDefeated(defeatedLootEnemy, 0.20);
        assertCondition(lootEngine.getPlayer().getBerries() == 0
                && lootEngine.getLootDrops().stream().anyMatch(LootDrop::isBerry)
                && lootEngine.getLootDrops().stream().anyMatch(drop -> "LOG_POSE".equals(drop.getType())),
                "Enemy berries and Log Poses appear as collectible world drops");
        String lootSnapshot = lootEngine.toJsonSnapshot();
        assertCondition(lootSnapshot.contains("\"lootDrops\":[{\"type\":\"BERRY\"")
                && lootSnapshot.contains("\"type\":\"LOG_POSE\""),
                "Loot drops are included in the game snapshot");
        lootEngine.tick(1.0 / 60.0);
        assertCondition(lootEngine.getPlayer().getBerries() == defeatedLootEnemy.getBerryDrop()
                && lootEngine.getPlayer().getGreenLogPoses() == 1,
                "Nearby dropped berries and Log Poses are collected automatically");
        assertCondition(lootEngine.getEnemies().stream().noneMatch(enemy ->
                enemy.getId() == defeatedLootEnemy.getId()),
                "Defeated enemies are removed from the active simulation");

        GameEngine returnEngine = new GameEngine();
        returnEngine.resetGame("Normal", "Return to Base Test");
        returnEngine.getPlayer().setPosition(2200, 1300);
        assertCondition("NO_LOG_POSE".equals(returnEngine.executePlayerAction("RETURN_TO_BASE", 0))
                && returnEngine.getPlayer().getX() == 2200,
                "Returning to base requires a Log Pose");
        returnEngine.getPlayer().addGreenLogPoses(1);
        assertCondition("RETURNED_TO_BASE".equals(returnEngine.executePlayerAction("RETURN_TO_BASE", 0))
                && returnEngine.getPlayer().getX() == 500
                && returnEngine.getPlayer().getY() == 1300
                && returnEngine.getPlayer().getGreenLogPoses() == 0,
                "Returning to base spends one Log Pose");
        GameEngine portalBossEngine = new GameEngine();
        portalBossEngine.resetGame("Normal", "Boss Portal Test");
        PirateEnemy portalBoss = portalBossEngine.getEnemies().stream()
                .filter(PirateEnemy::isBoss).findFirst().orElseThrow();
        assertCondition(portalBoss.getX() > 8700 && portalBoss.getY() < 2200,
                "IMU is isolated in the Empty Throne realm");
        portalBossEngine.getPlayer().setPosition(5100, 1300);
        assertCondition("GATE_LOCKED".equals(portalBossEngine.executePlayerAction("INTERACT", 0)),
                "The IMU portal stays locked until the section 5 keycard is collected");
        portalBossEngine.getPlayer().addImuKeycard();
        assertCondition("PORTAL_TRAVELLED".equals(portalBossEngine.executePlayerAction("INTERACT", 0))
                && "IMU".equals(portalBossEngine.getCurrentRealm())
                && portalBossEngine.toJsonSnapshot().contains("\"id\":\"imu_exit\"")
                && portalBossEngine.toJsonSnapshot().contains("\"realm\":\"IMU\""),
                "The section 5 portal leads to the isolated IMU realm");
        portalBossEngine.getPlayer().setPosition(9600, 1900);
        assertCondition("PORTAL_TRAVELLED".equals(portalBossEngine.executePlayerAction("INTERACT", 0))
                && "MAIN".equals(portalBossEngine.getCurrentRealm())
                && portalBossEngine.getPlayer().getX() == 5100,
                "The IMU realm portal returns to section 5");
        assertCondition(returnEngine.toJsonSnapshot().contains("\"portals\":[")
                && returnEngine.toJsonSnapshot().contains("\"realm\":\"MAIN\""),
                "Realm and portal state are included in the game snapshot");

        GameEngine imuKeycardEngine = new GameEngine();
        imuKeycardEngine.resetGame("Normal", "IMU Keycard Test");
        PirateEnemy keycardCarrier = imuKeycardEngine.getEnemies().stream()
                .filter(PirateEnemy::isImuKeycardCarrier).findFirst().orElseThrow();
        assertCondition(keycardCarrier.getX() >= 4578 && keycardCarrier.getX() < 5360
                        && imuKeycardEngine.getEnemies().stream()
                                .noneMatch(enemy -> enemy.getX() >= 5360 && enemy.getX() < 6200),
                "The keycard carrier is in the fifth and final section");
        keycardCarrier.takeDamage(keycardCarrier.getHp());
        imuKeycardEngine.handleEnemyDefeated(keycardCarrier, 0.99);
        imuKeycardEngine.tick(1.0 / 60.0);
        assertCondition(imuKeycardEngine.getLootDrops().stream()
                        .anyMatch(drop -> "IMU_KEYCARD".equals(drop.getType()))
                        && imuKeycardEngine.getEnemies().stream()
                                .noneMatch(enemy -> enemy.getId() == keycardCarrier.getId()),
                "The one-time section-five carrier drops an IMU Keycard");
        LootDrop imuCard = imuKeycardEngine.getLootDrops().stream()
                .filter(drop -> "IMU_KEYCARD".equals(drop.getType())).findFirst().orElseThrow();
        imuKeycardEngine.getPlayer().setPosition(imuCard.getX(), imuCard.getY());
        imuKeycardEngine.tick(1.0 / 60.0);
        assertCondition(imuKeycardEngine.getPlayer().getImuKeycards() == 1
                        && imuKeycardEngine.toJsonSnapshot().contains("\"imuKeycards\":1"),
                "Collecting the special drop adds the IMU Keycard to inventory");
        imuKeycardEngine.getPlayer().setPosition(5100, 1300);
        assertCondition("PORTAL_TRAVELLED".equals(imuKeycardEngine.executePlayerAction("INTERACT", 0))
                        && imuKeycardEngine.getPlayer().getImuKeycards() == 0,
                "The special keycard unlocks the IMU portal in section five");

        GameEngine harborEngine = new GameEngine();
        harborEngine.resetGame("Normal", "Safe Harbor Test");
        PirateEnemy harborEnemy = harborEngine.getEnemies().get(0);
        harborEnemy.setPosition(540, 1300);
        double harborHp = harborEngine.getPlayer().getHp();
        for (int i = 0; i < 120; i++) {
            harborEngine.tick(1.0 / 60.0);
        }
        assertCondition(harborEnemy.getX() == 540 && harborEnemy.getY() == 1300
                && harborEngine.getPlayer().getHp() == harborHp,
                "Enemies cannot approach or attack the player in base");

        PirateEnemy normalFireRateEnemy = sectionEngine.getEnemies().stream()
                .filter(enemy -> "Buggy Pirate".equals(enemy.getName())).findFirst().orElseThrow();
        assertCondition(Math.abs(normalFireRateEnemy.getFireRate() - (1.65 / 1.25 / 0.70)) < 0.000001,
                "Enemy firing frequency is reduced by 30 percent");
        assertCondition(GameEngine.DIFFICULTY_NAMES.length == 4
                && "Expert".equals(GameEngine.DIFFICULTY_NAMES[3])
                && "Normal".equals(GameEngine.DIFFICULTY_NAMES[
                        new GameEngine().resolveDifficultyIndex("Ludicrous")]),
                "Ludicrous is removed and unavailable difficulty names fall back to Normal");
        GameEngine easyEngine = new GameEngine();
        easyEngine.resetGame("Easy", "Difficulty Test");
        GameEngine normalEngine = new GameEngine();
        normalEngine.resetGame("Normal", "Difficulty Test");
        assertCondition(easyEngine.getEnemies().get(0).getMaxHp()
                < normalEngine.getEnemies().get(0).getMaxHp()
                && easyEngine.getEnemies().get(0).getDamage()
                < normalEngine.getEnemies().get(0).getDamage(),
                "Easy difficulty reduces enemy health and damage");
        for (int difficulty = 0; difficulty < GameEngine.DIFFICULTY_NAMES.length; difficulty++) {
            GameEngine scaledEngine = new GameEngine();
            scaledEngine.resetGame(GameEngine.DIFFICULTY_NAMES[difficulty], "Difficulty Test");
            PirateEnemy surfaceEnemy = scaledEngine.getEnemies().stream()
                    .filter(enemy -> "Buggy Pirate".equals(enemy.getName())).findFirst().orElseThrow();
            PirateEnemy boss = scaledEngine.getEnemies().stream()
                    .filter(PirateEnemy::isBoss).findFirst().orElseThrow();
            int expectedSurfaceDamage = (int) Math.round(12 * GameEngine.DIFFICULTY_DMG_MULT[difficulty]);
            int expectedBossDamage = (int) Math.round(34 * GameEngine.DIFFICULTY_DMG_MULT[difficulty]);
            double[] previousDamageMultipliers = {0.40, 0.65, 1.20, 2.20};
            assertCondition(GameEngine.DIFFICULTY_DMG_MULT[difficulty]
                            == previousDamageMultipliers[difficulty] * 0.5
                    && surfaceEnemy.getDamage() == expectedSurfaceDamage
                    && boss.getDamage() == expectedBossDamage,
                    GameEngine.DIFFICULTY_NAMES[difficulty]
                            + " enemies and bosses deal 50% less damage");
            assertCondition(hasExactSectionEnemyCounts(scaledEngine, difficulty + 1),
                    GameEngine.DIFFICULTY_NAMES[difficulty]
                            + " has " + ((difficulty + 1) * 2) + " enemies in each surface section");
        }

        GameEngine chestEngine = new GameEngine();
        chestEngine.resetGame("Normal", "Test Captain");
        RarityChest nearbyChest = chestEngine.getChests().get(0);
        chestEngine.getPlayer().setPosition(nearbyChest.getX(), nearbyChest.getY());
        chestEngine.tick(1.0 / 60.0);
        assertCondition(!nearbyChest.isOpened(), "Walking over a chest does not open it automatically");
        chestEngine.executePlayerAction("INTERACT", 0);
        assertCondition(nearbyChest.isOpened(), "Nearby chests open through the interact action");
        assertCondition(chestEngine.getPlayer().getGoldKeycards() == 0,
                "Chests do not award Gold Keys");
        nearbyChest.update(39.99);
        assertCondition(nearbyChest.isOpened(), "Opened chests stay closed for nearly 40 seconds");
        nearbyChest.update(0.02);
        assertCondition(!nearbyChest.isOpened(), "Opened chests return after 40 seconds");
        int initialChestCount = chestEngine.getChests().size();
        chestEngine.resetGame("Normal", "Second Chest Spawn Test");
        assertCondition(chestEngine.getChests().size() == initialChestCount
                && chestEngine.getChests().stream().allMatch(chest -> !chest.isOpened()),
                "Chests spawn again after restarting a run");
        chestEngine.resetGame("Expert", "Game Over Chest Spawn Test");
        chestEngine.getPlayer().setPosition(1300, 1100);
        chestEngine.getProjectiles().add(new Projectile(1300, 1100, 0, 0, 10,
                1000, 1.0, false, false, "#f87171", "#f87171"));
        chestEngine.tick(1.0 / 60.0);
        assertCondition("DEFEAT".equals(chestEngine.getGameMode()),
                "Test run reaches game over before restarting");
        chestEngine.resetGame("Normal", "Post Game Over Chest Spawn Test");
        assertCondition(chestEngine.getChests().size() == initialChestCount,
                "Chests spawn after game over");
        chestEngine.resetGame("Normal", "Third Chest Spawn Test");
        assertCondition(chestEngine.getChests().size() == initialChestCount,
                "Chest spawning remains reliable on a second restart");

        assertCondition(!testPlayer.buyWeapon(4), "Purchase rejected when berries < cost");

        testPlayer.applyChestRewards(9999, 9999, 50, false);
        assertCondition(testPlayer.getHp() == testPlayer.getMaxHp(), "HP clamped at maxHp");
        assertCondition(testPlayer.getEnergy() == testPlayer.getMaxEnergy(), "Energy clamped at maxEnergy");

        testPlayer.takeDamage(-50.0);
        assertCondition(testPlayer.getHp() == testPlayer.getMaxHp(), "Negative damage safely ignored");
        testPlayer.takeDamage(9999.0);
        assertCondition(testPlayer.getHp() == 0.0, "HP floored cleanly at 0.0");

        assertCondition(!testPlayer.selectWeaponSlot(-1), "Negative weapon slot rejected");
        assertCondition(!testPlayer.selectWeaponSlot(99), "Out-of-range weapon slot rejected");

        GameEngine engine = new GameEngine();
        assertCondition("Normal".equals(GameEngine.DIFFICULTY_NAMES[
                engine.resolveDifficultyIndex("InvalidMode")]), "Unknown difficulty defaults to Normal");
        assertCondition(GameEngine.DIFFICULTY_NAMES.length == 4, "Only four difficulty levels are available");

        System.out.println("=== All Boundary & Reliability Assertions PASSED! ===");
    }

    private static void assertCondition(boolean condition, String label) {
        if (!condition) {
            throw new IllegalStateException("Boundary test failed: " + label);
        }
        System.out.println("  [PASS] " + label);
    }

    private static boolean hasExactSectionEnemyCounts(GameEngine engine, int gateRequirement) {
        int[] sectionMinX = {260, 1778, 2728, 3628, 4578};
        int[] sectionMaxX = {1740, 2690, 3590, 4540, 5360};
        for (int i = 0; i < sectionMinX.length; i++) {
            final int sectionStart = sectionMinX[i];
            final int sectionEnd = sectionMaxX[i];
            long enemiesInSection = engine.getEnemies().stream()
                    .filter(enemy -> !enemy.isBoss()
                            && enemy.getY() < 2140
                            && enemy.getX() >= sectionStart && enemy.getX() < sectionEnd)
                    .count();
            long expectedCount = 2L * gateRequirement;
            if (enemiesInSection != expectedCount) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasChestInEveryMapSection(GameEngine engine) {
        int[] sectionMinX = {260, 1778, 2728, 3628, 4578};
        int[] sectionMaxX = {1740, 2690, 3590, 4540, 5360};
        for (int i = 0; i < sectionMinX.length; i++) {
            final int sectionStart = sectionMinX[i];
            final int sectionEnd = sectionMaxX[i];
            boolean hasChest = engine.getChests().stream().anyMatch(chest ->
                    chest.getY() >= 460 && chest.getY() <= 2140
                            && chest.getX() >= sectionStart && chest.getX() < sectionEnd);
            if (!hasChest) {
                return false;
            }
        }
        boolean hasHarborChest = engine.getChests().stream().anyMatch(chest ->
                chest.getX() <= 800 && chest.getY() >= 1120 && chest.getY() <= 1680);
        boolean hasThroneChest = engine.getChests().stream().anyMatch(chest ->
                chest.getX() >= 8700 && chest.getX() < 10600
                        && chest.getY() >= 600 && chest.getY() < 2200);
        return hasHarborChest && hasThroneChest;
    }

    private static boolean overlapsSectionObstacle(GameEngine engine, double x, double y, double radius) {
        return engine.getWalls().stream()
                .filter(wall -> "crate".equals(wall.getMaterialType())
                        || "tree".equals(wall.getMaterialType()))
                .anyMatch(wall -> {
                    double closestX = Math.max(wall.getX(), Math.min(x, wall.getX() + wall.getWidth()));
                    double closestY = Math.max(wall.getY(), Math.min(y, wall.getY() + wall.getHeight()));
                    double dx = x - closestX;
                    double dy = y - closestY;
                    return dx * dx + dy * dy < radius * radius;
                });
    }
}
