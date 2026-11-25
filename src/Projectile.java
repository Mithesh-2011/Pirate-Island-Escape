import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Projectile {
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private final long id;
    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final double radius;
    private final int damage;
    private double remainingLife;
    private final boolean fromPlayer;
    private final boolean piercing;
    private final String color;
    private final String rarityColor;
    private final List<Integer> hitEnemyIds;

    public Projectile(double x, double y, double vx, double vy, double radius, int damage,
                      double life, boolean fromPlayer, boolean piercing,
                      String color, String rarityColor) {
        this.id = NEXT_ID.incrementAndGet();
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.damage = damage;
        this.remainingLife = life;
        this.fromPlayer = fromPlayer;
        this.piercing = piercing;
        this.color = color;
        this.rarityColor = rarityColor;
        this.hitEnemyIds = new ArrayList<>();
    }

    public void advanceLifetime(double dt) {
        remainingLife -= dt;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean hasAlreadyHit(int enemyId) {
        for (int i = 0; i < hitEnemyIds.size(); i++) {
            if (hitEnemyIds.get(i) == enemyId) {
                return true;
            }
        }
        return false;
    }

    public void recordHit(int enemyId) {
        hitEnemyIds.add(enemyId);
    }

    public boolean isExpired() { return remainingLife <= 0.0; }
    public long getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getRadius() { return radius; }
    public int getDamage() { return damage; }
    public boolean isFromPlayer() { return fromPlayer; }
    public boolean isPiercing() { return piercing; }
    public String getColor() { return color; }
    public String getRarityColor() { return rarityColor; }
}
