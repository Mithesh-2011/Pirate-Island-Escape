import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private final int port;
    private final GameEngine engine;
    private HttpServer server;
    private ScheduledExecutorService ticker;

    public GameServer(int port, GameEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/", new StaticHtmlHandler());
        server.createContext("/health", new HealthHandler());

        server.createContext("/api/state", new StateHandler());
        server.createContext("/api/start", new StartHandler());
        server.createContext("/api/input", new InputHandler());
        server.createContext("/api/action", new ActionHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        ticker = Executors.newSingleThreadScheduledExecutor();
        ticker.scheduleAtFixedRate(() -> {
            try {
                engine.tick(1.0 / 60.0);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private class StaticHtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File htmlFile = new File("web/index.html");
            if (!htmlFile.exists()) {
                htmlFile = new File("../web/index.html");
            }
            if (!htmlFile.exists()) {
                byte[] msg = "Missing web/index.html".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg);
                }
                return;
            }
            byte[] bytes = readAllBytes(htmlFile);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJson(exchange, 200, engine.toJsonSnapshot());
        }
    }

    private class StartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            String difficulty = extractJsonString(body, "difficulty", "Normal");
            String callsign = extractJsonString(body, "callsign", "Monkey D. Luffy");
            engine.resetGame(difficulty, callsign);
            sendJson(exchange, 200, engine.toJsonSnapshot());
        }
    }

    private class InputHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            boolean up = extractJsonBoolean(body, "up");
            boolean down = extractJsonBoolean(body, "down");
            boolean left = extractJsonBoolean(body, "left");
            boolean right = extractJsonBoolean(body, "right");
            boolean firing = extractJsonBoolean(body, "firing");
            double aimAngle = extractJsonDouble(body, "aimAngle", 0.0);
            try {
                double playerX = extractJsonDouble(body, "playerX", Double.NaN);
                double playerY = extractJsonDouble(body, "playerY", Double.NaN);
                if (!Double.isFinite(playerX) || !Double.isFinite(playerY)) {
                    throw new IllegalArgumentException("Missing player position");
                }
                Map<Long, double[]> enemyUpdates = parsePositionUpdates(
                        extractJsonString(body, "enemyPositions", ""));
                Map<Integer, double[]> enemyPositions = new HashMap<>();
                for (Map.Entry<Long, double[]> entry : enemyUpdates.entrySet()) {
                    enemyPositions.put(Math.toIntExact(entry.getKey()), entry.getValue());
                }
                Map<Long, double[]> projectilePositions = parsePositionUpdates(
                        extractJsonString(body, "projectilePositions", ""));
                engine.updateInputState(up, down, left, right, firing, aimAngle);
                engine.updateClientMovement(playerX, playerY, enemyPositions, projectilePositions);
                sendJson(exchange, 200, "{\"ok\":true}");
            } catch (IllegalArgumentException | ArithmeticException ex) {
                sendJson(exchange, 400, "{\"error\":\"Invalid movement update\"}");
            }
        }
    }

    private class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            String action = extractJsonString(body, "action", "");
            int param = (int) Math.round(extractJsonDouble(body, "param", 0.0));
            double aimAngle = extractJsonDouble(body, "aimAngle", Double.NaN);
            String result = engine.executePlayerAction(action, param, aimAngle);
            sendJson(exchange, 200, "{\"result\":\"" + result + "\"}");
        }
    }

    private String extractJsonString(String json, String key, String fallback) {
        int start = jsonValueStart(json, key);
        if (start < 0 || start >= json.length() || json.charAt(start) != '"') return fallback;
        int end = start + 1;
        boolean escaped = false;
        while (end < json.length()) {
            char current = json.charAt(end);
            if (current == '"' && !escaped) {
                return json.substring(start + 1, end);
            }
            if (current == '\\' && !escaped) {
                escaped = true;
            } else {
                escaped = false;
            }
            end++;
        }
        return fallback;
    }

    private boolean extractJsonBoolean(String json, String key) {
        int start = jsonValueStart(json, key);
        return start >= 0 && json.startsWith("true", start);
    }

    private double extractJsonDouble(String json, String key, double fallback) {
        int start = jsonValueStart(json, key);
        if (start < 0) return fallback;
        int end = start;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if ((ch >= '0' && ch <= '9') || ch == '.' || ch == '-' || ch == '+'
                    || ch == 'e' || ch == 'E') {
                end++;
            } else {
                break;
            }
        }
        if (end == start) return fallback;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int jsonValueStart(String json, String key) {
        if (json == null) return -1;
        int keyStart = json.indexOf("\"" + key + "\"");
        if (keyStart < 0) return -1;
        int colon = json.indexOf(':', keyStart + key.length() + 2);
        if (colon < 0) return -1;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        return start;
    }

    private Map<Long, double[]> parsePositionUpdates(String updates) {
        Map<Long, double[]> positions = new HashMap<>();
        if (updates.isEmpty()) return positions;
        for (String record : updates.split(";")) {
            String[] values = record.split(",", -1);
            if (values.length != 3) {
                throw new IllegalArgumentException("Invalid position record");
            }
            long id = Long.parseLong(values[0]);
            double x = Double.parseDouble(values[1]);
            double y = Double.parseDouble(values[2]);
            if (id <= 0 || !Double.isFinite(x) || !Double.isFinite(y)) {
                throw new IllegalArgumentException("Invalid position value");
            }
            positions.put(id, new double[]{x, y});
        }
        return positions;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    private byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
