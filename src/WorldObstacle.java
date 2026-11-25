public class WorldObstacle {
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final String materialType;

    public WorldObstacle(double x, double y, double width, double height, String materialType) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.materialType = (materialType != null && materialType.length() > 0) ? materialType : "rock";
    }

    public double[] resolveCircleCollision(double cx, double cy, double radius) {
        double closestX = Math.max(x, Math.min(cx, x + width));
        double closestY = Math.max(y, Math.min(cy, y + height));

        double dx = cx - closestX;
        double dy = cy - closestY;
        double distSq = dx * dx + dy * dy;

        if (distSq < radius * radius) {
            double dist = Math.sqrt(distSq);
            if (dist > 0.0001) {
                double overlap = radius - dist;
                cx += (dx / dist) * overlap;
                cy += (dy / dist) * overlap;
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

    public boolean containsPoint(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public String getMaterialType() { return materialType; }
}
