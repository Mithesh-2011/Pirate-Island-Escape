public class PirateEnemy {
    private final int id;
    private final String type;
    private final String name;
    private final double radius;
    private final double speed;
    private final int damage;
    private final double fireRate;
    private final double attackRange;
    private final int berryDrop;
    private final int greenKeyDrop;
    private final int goldKeyDrop;
    private final String primaryColor;
    private final String accentColor;
    private final boolean boss;

    private double x;
    private double y;
    private int hp;
    private final int maxHp;
    private double attackTimer;
    private double specialTimer;
    private double hitFlashTimer;
    private double aimAngle;

    public PirateEnemy(int id, String type, String name, double x, double y, double radius,
                       int baseHp, double speed, int baseDamage, double fireRate, double attackRange,
                       int berryDrop, int greenKeyDrop, int goldKeyDrop,
                       String primaryColor, String accentColor, boolean boss,
                       double hpMultiplier, double dmgMultiplier) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.maxHp = Math.max(1, (int) Math.round(baseHp * hpMultiplier));
        this.hp = this.maxHp;
        this.speed = speed;
        this.damage = Math.max(1, (int) Math.round(baseDamage * dmgMultiplier));
        this.fireRate = fireRate;
        this.attackRange = attackRange;
        this.berryDrop = berryDrop > 0 ? (int) Math.round(berryDrop * 1.15) : 0;
        this.greenKeyDrop = greenKeyDrop;
        this.goldKeyDrop = goldKeyDrop;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
        this.boss = boss;

        this.attackTimer = 0.6 + Math.random() * 0.8;
        this.specialTimer = 3.5;
        this.hitFlashTimer = 0.0;
        this.aimAngle = Math.PI;
    }

    private PirateEnemy(int id, PirateEnemy source) {
        this.id = id;
        this.type = source.type;
        this.name = source.name;
        this.x = source.x;
        this.y = source.y;
        this.radius = source.radius;
        this.hp = source.maxHp;
        this.maxHp = source.maxHp;
        this.speed = source.speed;
        this.damage = source.damage;
        this.fireRate = source.fireRate;
        this.attackRange = source.attackRange;
        this.berryDrop = source.berryDrop;
        this.greenKeyDrop = source.greenKeyDrop;
        this.goldKeyDrop = source.goldKeyDrop;
        this.primaryColor = source.primaryColor;
        this.accentColor = source.accentColor;
        this.boss = source.boss;
        this.attackTimer = 0.6 + Math.random() * 0.8;
        this.specialTimer = 3.5;
        this.hitFlashTimer = 0.0;
        this.aimAngle = Math.PI;
    }

    public void takeDamage(int amount) {
        if (amount <= 0 || hp <= 0) {
            return;
        }
        hp = Math.max(0, hp - amount);
        hitFlashTimer = 0.10;
    }

    public int getBossPhase() {
        double ratio = (double) hp / Math.max(1, maxHp);
        if (ratio <= 0.35) {
            return 3;
        } else if (ratio <= 0.70) {
            return 2;
        } else {
            return 1;
        }
    }

    public int compareTitleTo(PirateEnemy other) {
        if (other == null) {
            return 1;
        }
        return this.name.compareTo(other.name);
    }

    public boolean isAlive() { return hp > 0; }
    public PirateEnemy respawnAs(int newId) { return new PirateEnemy(newId, this); }
    public void setPosition(double nx, double ny) { this.x = nx; this.y = ny; }
    public void setAimAngle(double angle) { this.aimAngle = angle; }
    public void tickTimers(double dt) {
        if (hitFlashTimer > 0) hitFlashTimer = Math.max(0, hitFlashTimer - dt);
        attackTimer -= dt;
        specialTimer -= dt;
    }
    public void resetAttackTimer() { attackTimer = fireRate; }
    public void resetSpecialTimer(double seconds) { specialTimer = seconds; }

    public int getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public double getRadius() { return radius; }
    public double getSpeed() { return speed; }
    public int getDamage() { return damage; }
    public double getFireRate() { return fireRate; }
    public double getAttackRange() { return attackRange; }
    public int getBerryDrop() { return berryDrop; }
    public int getGreenKeyDrop() { return greenKeyDrop; }
    public int getGoldKeyDrop() { return goldKeyDrop; }
    public String getPrimaryColor() { return primaryColor; }
    public String getAccentColor() { return accentColor; }
    public boolean isBoss() { return boss; }
    public boolean isBossCardCarrier() { return "boss_card_holder".equals(type); }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public double getAttackTimer() { return attackTimer; }
    public double getSpecialTimer() { return specialTimer; }
    public double getHitFlashTimer() { return hitFlashTimer; }
    public double getAimAngle() { return aimAngle; }
}
