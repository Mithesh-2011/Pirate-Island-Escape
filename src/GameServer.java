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
            engine.updateInputState(up, down, left, right, firing, aimAngle);
            sendJson(exchange, 200, "{\"ok\":true}");
        }
    }

    private class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            String action = extractJsonString(body, "action", "");
            int param = (int) Math.round(extractJsonDouble(body, "param", 0.0));
            String result = engine.executePlayerAction(action, param);
            sendJson(exchange, 200, "{\"result\":\"" + result + "\"}");
        }
    }

    private String extractJsonString(String json, String key, String fallback) {
        if (json == null) return fallback;
        String token = "\"" + key + "\":\"";
        int idx = json.indexOf(token);
        if (idx == -1) return fallback;
        int start = idx + token.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return fallback;
        return json.substring(start, end);
    }

    private boolean extractJsonBoolean(String json, String key) {
        if (json == null) return false;
        String token = "\"" + key + "\":true";
        return json.indexOf(token) != -1;
    }

    private double extractJsonDouble(String json, String key, double fallback) {
        if (json == null) return fallback;
        String token = "\"" + key + "\":";
        int idx = json.indexOf(token);
        if (idx == -1) return fallback;
        int start = idx + token.length();
        int end = start;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if ((ch >= '0' && ch <= '9') || ch == '.' || ch == '-') {
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
