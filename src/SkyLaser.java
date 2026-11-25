public class SkyLaser {
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final double cycleDuration;
    private double timer;
    private final double warnDuration;
    private final double fireDuration;
    private final double width;
    private final boolean oneShot;
    private final int damage;

    public SkyLaser(double x1, double y1, double x2, double y2, double cycleDuration,
                    double initialTimer, double warnDuration, double fireDuration,
                    double width, boolean oneShot, int damage) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.cycleDuration = cycleDuration;
        this.timer = initialTimer;
        this.warnDuration = warnDuration;
        this.fireDuration = fireDuration;
        this.width = width;
        this.oneShot = oneShot;
        this.damage = damage;
    }

    public void update(double dt) {
        timer += dt;
        if (!oneShot && cycleDuration > 0.0 && timer >= cycleDuration) {
            timer %= cycleDuration;
        }
    }

    public boolean isFinishedOneShot() {
        return oneShot && timer >= (warnDuration + fireDuration);
    }

    public boolean isWarning() {
        return timer < warnDuration;
    }

    public boolean isFiring() {
        return timer >= warnDuration && timer < (warnDuration + fireDuration);
    }

    public double distanceToPoint(double px, double py) {
        double l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (l2 == 0.0) {
            return Math.hypot(px - x1, py - y1);
        }
        double t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0.0, Math.min(1.0, t));
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);
        return Math.hypot(px - projX, py - projY);
    }

    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    public double getWidth() { return width; }
    public int getDamage() { return damage; }
}
