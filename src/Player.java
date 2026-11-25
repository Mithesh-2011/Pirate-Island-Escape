import java.util.ArrayList;
import java.util.List;

public class Player {
    private String captainCallsign;
    private double x;
    private double y;
    private final double radius;
    private double hp;
    private double maxHp;
    private double energy;
    private double maxEnergy;
    private int berries;
    private int greenLogPoses;
    private int goldKeycards;
    private int imuKeycards;
    private boolean escapeKey;
    private boolean gear5Active;
    private double aimAngle;
    private double walkBob;

    private double dashTimer;
    private double dashCooldown;
    private double dashDirX;
    private double dashDirY;
    private double invulnTimer;

    private int equippedWeaponIndex;
    private final List<Weapon> weapons;

    public Player(String rawCallsign, double startX, double startY) {
        this.captainCallsign = formatCaptainCallsign(rawCallsign);
        this.x = startX;
        this.y = startY;
        this.radius = 20.0;
        this.hp = 100.0;
        this.maxHp = 100.0;
        this.energy = 100.0;
        this.maxEnergy = 100.0;
        this.berries = 0;
        this.greenLogPoses = 0;
        this.goldKeycards = 0;
        this.imuKeycards = 0;
        this.escapeKey = false;
        this.gear5Active = false;
        this.aimAngle = 0.0;
        this.walkBob = 0.0;
        this.dashTimer = 0.0;
        this.dashCooldown = 0.0;
        this.invulnTimer = 0.0;
        this.equippedWeaponIndex = 0;
        this.weapons = new ArrayList<>();
        initializeArsenal();
    }

    public String formatCaptainCallsign(String raw) {
        if (raw == null || raw.trim().length() == 0) {
            return "Straw Hat Luffy";
        }
        String cleaned = raw.trim();

        int colonIdx = cleaned.indexOf(":");
        if (colonIdx != -1) {
            cleaned = cleaned.substring(0, colonIdx).trim();
        }
        if (cleaned.length() > 18) {
            cleaned = cleaned.substring(0, 18);
        }
        if (cleaned.equals("Luffy")) {
            return "Monkey D. Luffy";
        }
        return cleaned;
    }

    public boolean outranksLexicographically(String rivalTitle) {
        if (rivalTitle == null) {
            return true;
        }
        return this.captainCallsign.compareTo(rivalTitle) < 0;
    }

    private void initializeArsenal() {
        weapons.clear();
        weapons.add(new Weapon("pistol", "Gomu-Gomu Pistol", "Standard", "FIST", 0,
                12, 3.0, 0.22, 24, 740, 8, 0.85, 0.0, 1, false,
                "#fbbf24", "Fast rubber punch volley. Starter weapon.", true));

        weapons.add(new Weapon("kabuto", "Kabuto Launcher", "Ranged", "BOW", 15,
                5, 3.0, 0.38, 58, 860, 10, 1.05, 0.0, 1, false,
                "#f97316", "High-velocity Firebird Star with heavy single-target impact.", false));

        weapons.add(new Weapon("bento", "Diable Bento Launcher", "Blaze", "FIRE", 28,
                12, 3.0, 0.44, 28, 640, 9, 0.75, 0.24, 3, false,
                "#ef4444", "Launches a 3-way blazing spread of spicy projectiles.", false));

        weapons.add(new Weapon("opeorb", "Ope-Ope Room Orb", "Room", "ORB", 45,
                1, 3.0, 0.65, 115, 340, 26, 2.10, 0.0, 1, true,
                "#38bdf8", "Massive piercing ROOM sphere that slices through pirate squads.", false));

        weapons.add(new Weapon("radical", "Radical Laser Beam", "Laser", "LASER", 68,
                4, 3.0, 0.30, 88, 1120, 11, 1.15, 0.0, 1, true,
                "#22d3ee", "Super-fast piercing laser beam that melts armored Admirals.", false));

    }

    public void activateGear5() {
        if (gear5Active) return;
        gear5Active = true;
        maxHp = 150.0;
        hp = Math.min(maxHp, hp + 50.0);
    }

    public boolean tryDash(double inputDx, double inputDy) {
        if (dashTimer > 0.0 || dashCooldown > 0.0 || energy < 25.0) {
            return false;
        }
        energy -= 25.0;
        dashTimer = 0.18;
        dashCooldown = 0.50;
        invulnTimer = 0.25;

        if (inputDx == 0.0 && inputDy == 0.0) {
            dashDirX = Math.cos(aimAngle);
            dashDirY = Math.sin(aimAngle);
        } else {
            double len = Math.hypot(inputDx, inputDy);
            dashDirX = inputDx / len;
            dashDirY = inputDy / len;
        }
        return true;
    }

    public void applyChestRewards(int hpGain, int energyGain, int berryGain, boolean overhealBonus) {
        double hpLimit = gear5Active ? 150.0 : 160.0;
        if (overhealBonus && maxHp < hpLimit) {
            maxHp = Math.min(hpLimit, maxHp + 15.0);
        }
        hp = Math.min(maxHp, hp + Math.max(0, hpGain));
        energy = Math.min(maxEnergy, energy + Math.max(0, energyGain));
        berries += Math.max(0, berryGain);
    }

    public boolean takeDamage(double rawDamage) {
        if (rawDamage <= 0.0 || invulnTimer > 0.0 || hp <= 0.0) {
            return false;
        }
        hp = Math.max(0.0, hp - rawDamage);
        invulnTimer = 0.45;
        return true;
    }

    public boolean selectWeaponSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= weapons.size()) {
            return false;
        }
        Weapon target = weapons.get(slotIndex);
        if (!target.isUnlocked()) {
            return false;
        }
        equippedWeaponIndex = slotIndex;
        return true;
    }

    public boolean buyWeapon(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= weapons.size()) {
            return false;
        }
        Weapon w = weapons.get(slotIndex);
        if (w.isUnlocked() || w.getUnlockCost() < 0 || berries < w.getUnlockCost()) {
            return false;
        }
        berries -= w.getUnlockCost();
        w.unlockWithRarity(0);
        equippedWeaponIndex = slotIndex;
        return true;
    }

    public boolean upgradeWeaponRarity(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= weapons.size()) {
            return false;
        }
        Weapon w = weapons.get(slotIndex);
        int cost = w.getNextUpgradeCost();
        if (!w.isUnlocked() || w.isMaxRarity() || berries < cost) {
            return false;
        }
        berries -= cost;
        return w.upgradeRarity();
    }

    public double distanceTo(double tx, double ty) {
        return Math.hypot(x - tx, y - ty);
    }

    public Weapon getEquippedWeapon() {
        return weapons.get(equippedWeaponIndex);
    }

    public void addBerries(int amount) { if (amount > 0) berries += amount; }
    public boolean spendBerries(int amount) {
        if (amount < 0 || berries < amount) return false;
        berries -= amount;
        return true;
    }
    public boolean consumeEnergy(double amount) {
        if (amount < 0.0 || energy < amount) return false;
        energy -= amount;
        return true;
    }
    public void addGreenLogPoses(int amount) { if (amount > 0) greenLogPoses += amount; }
    public void spendGreenLogPoses(int amount) { greenLogPoses = Math.max(0, greenLogPoses - amount); }
    public void addGoldKeycard() { goldKeycards = Math.min(3, goldKeycards + 1); }
    public void spendGoldKeycards(int amount) { goldKeycards = Math.max(0, goldKeycards - amount); }
    public void addImuKeycard() { imuKeycards = Math.min(1, imuKeycards + 1); }
    public void spendImuKeycards(int amount) { imuKeycards = Math.max(0, imuKeycards - amount); }
    public void addEscapeKey() { escapeKey = true; }

    public void setPosition(double nx, double ny) { this.x = nx; this.y = ny; }
    public void setAimAngle(double angle) { this.aimAngle = angle; }
    public void regeneratePassiveEnergy(double dt) {
        energy = Math.min(maxEnergy, energy + dt * (gear5Active ? 2.0 : 1.0));
    }
    public void tickTimers(double dt) {
        if (dashTimer > 0) dashTimer = Math.max(0, dashTimer - dt);
        if (dashCooldown > 0) dashCooldown = Math.max(0, dashCooldown - dt);
        if (invulnTimer > 0) invulnTimer = Math.max(0, invulnTimer - dt);
        for (int i = 0; i < weapons.size(); i++) {
            weapons.get(i).updateTimers(dt);
        }
    }
    public void incrementWalkBob(double dt) { walkBob += dt * 14.0; }
    public void resetWalkBob() { walkBob = 0.0; }

    public String getCaptainCallsign() { return captainCallsign; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public double getHp() { return hp; }
    public double getMaxHp() { return maxHp; }
    public double getEnergy() { return energy; }
    public double getMaxEnergy() { return maxEnergy; }
    public int getBerries() { return berries; }
    public int getGreenLogPoses() { return greenLogPoses; }
    public int getGoldKeycards() { return goldKeycards; }
    public int getImuKeycards() { return imuKeycards; }
    public boolean hasEscapeKey() { return escapeKey; }
    public boolean hasGear5() { return gear5Active; }
    public double getDamageMultiplier() { return gear5Active ? 1.4 : 1.0; }
    public double getAimAngle() { return aimAngle; }
    public double getWalkBob() { return walkBob; }
    public boolean isDashing() { return dashTimer > 0.0; }
    public double getDashDirX() { return dashDirX; }
    public double getDashDirY() { return dashDirY; }
    public int getEquippedWeaponIndex() { return equippedWeaponIndex; }
    public List<Weapon> getWeapons() { return weapons; }
}
