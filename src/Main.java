import java.awt.Desktop;
import java.net.URI;

public class Main {
    public static void main(String[] args) {
        try {
            BoundaryTestSuite.runAllTests();

            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            GameEngine engine = new GameEngine();
            GameServer server = new GameServer(port, engine);
            server.start();

            String url = "http://localhost:" + port;
            System.out.println("\n==============================================================");
            System.out.println("  GRAND LINE ESCAPE — JAVA BACKEND SERVER RUNNING");
            System.out.println("  Open in your browser: " + url);
            System.out.println("  Press Ctrl+C in this terminal to stop the server.");
            System.out.println("==============================================================\n");

            if (System.getenv("PORT") == null && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.err.println("Failed to launch Grand Line Escape: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
