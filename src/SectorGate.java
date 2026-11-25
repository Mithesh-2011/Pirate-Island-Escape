public class SectorGate {
    private final String id;
    private final String name;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final int requiredGreenKeys;
    private final int requiredGoldKeys;
    private final int requiredSpecialKeys;
    private final String hexColor;
    private boolean opened;

    public SectorGate(String id, String name, double x, double y, double width, double height,
                      int requiredGreenKeys, int requiredGoldKeys, String hexColor) {
        this(id, name, x, y, width, height, requiredGreenKeys, requiredGoldKeys, 0, hexColor);
    }

    public SectorGate(String id, String name, double x, double y, double width, double height,
                      int requiredGreenKeys, int requiredGoldKeys, int requiredSpecialKeys,
                      String hexColor) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.requiredGreenKeys = Math.max(0, requiredGreenKeys);
        this.requiredGoldKeys = Math.max(0, requiredGoldKeys);
        this.requiredSpecialKeys = Math.max(0, requiredSpecialKeys);
        this.hexColor = hexColor;
        this.opened = false;
    }

    public boolean canUnlock(int playerGreenKeys, int playerGoldKeys) {
        return canUnlock(playerGreenKeys, playerGoldKeys, 0);
    }

    public boolean canUnlock(int playerGreenKeys, int playerGoldKeys, int playerSpecialKeys) {
        if (opened) {
            return false;
        }
        if (requiredSpecialKeys > 0) {
            return playerSpecialKeys >= requiredSpecialKeys;
        }
        if (requiredGoldKeys > 0) {
            return playerGoldKeys >= requiredGoldKeys;
        }
        return playerGreenKeys >= requiredGreenKeys;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public double getCenterX() { return x + width / 2.0; }
    public double getCenterY() { return y + height / 2.0; }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int getRequiredGreenKeys() { return requiredGreenKeys; }
    public int getRequiredGoldKeys() { return requiredGoldKeys; }
    public int getRequiredSpecialKeys() { return requiredSpecialKeys; }
    public String getHexColor() { return hexColor; }
    public boolean isOpened() { return opened; }
}
