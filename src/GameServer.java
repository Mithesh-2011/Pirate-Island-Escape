import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameServer {
    private static final int TICKS_PER_SECOND = 30;
    private static final int SNAPSHOTS_PER_SECOND = 15;
    private static final int MAX_WEBSOCKET_MESSAGE_SIZE = 64 * 1024;

    private final int port;
    private final GameEngine engine;
    private final Set<Session> webSocketSessions = ConcurrentHashMap.newKeySet();
    private final Map<Session, AtomicBoolean> pendingSnapshots = new ConcurrentHashMap<>();
    private Server server;
    private ScheduledExecutorService ticker;
    private ScheduledExecutorService snapshotPublisher;

    public GameServer(int port, GameEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    public void start() throws Exception {
        server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new StaticHtmlServlet()), "/");
        context.addServlet(new ServletHolder(new HealthServlet()), "/health");
        context.addServlet(new ServletHolder(new StateServlet()), "/api/state");
        context.addServlet(new ServletHolder(new StartServlet()), "/api/start");
        context.addServlet(new ServletHolder(new InputServlet()), "/api/input");
        context.addServlet(new ServletHolder(new ActionServlet()), "/api/action");

        JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
            container.setDefaultMaxTextMessageBufferSize(MAX_WEBSOCKET_MESSAGE_SIZE);
            container.addEndpoint(ServerEndpointConfig.Builder
                    .create(GameWebSocketEndpoint.class, "/ws")
                    .configurator(new ServerEndpointConfig.Configurator() {
                        @Override
                        public <T> T getEndpointInstance(Class<T> endpointClass) {
                            return endpointClass.cast(new GameWebSocketEndpoint(GameServer.this));
                        }
                    })
                    .build());
        });

        server.setHandler(context);
        server.start();

        ticker = Executors.newSingleThreadScheduledExecutor();
        ticker.scheduleAtFixedRate(() -> {
            try {
                engine.tick(1.0 / TICKS_PER_SECOND);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 0, TimeUnit.SECONDS.toNanos(1) / TICKS_PER_SECOND, TimeUnit.NANOSECONDS);

        snapshotPublisher = Executors.newSingleThreadScheduledExecutor();
        snapshotPublisher.scheduleAtFixedRate(this::publishSnapshots, 0,
                1_000 / SNAPSHOTS_PER_SECOND, TimeUnit.MILLISECONDS);
    }

    private void publishSnapshots() {
        if (webSocketSessions.isEmpty()) return;
        String snapshot = engine.toJsonSnapshot();
        for (Session session : webSocketSessions) {
            sendSnapshot(session, snapshot);
        }
    }

    private void sendSnapshot(Session session, String snapshot) {
        AtomicBoolean pending = pendingSnapshots.get(session);
        if (pending == null || !session.isOpen() || !pending.compareAndSet(false, true)) return;
        try {
            session.getAsyncRemote().sendText(snapshot, result -> {
                pending.set(false);
                if (!result.isOK()) removeWebSocketSession(session);
            });
        } catch (IllegalStateException ex) {
            pending.set(false);
            removeWebSocketSession(session);
        }
    }

    private void addWebSocketSession(Session session) {
        webSocketSessions.add(session);
        pendingSnapshots.put(session, new AtomicBoolean());
        sendSnapshot(session, engine.toJsonSnapshot());
    }

    private void removeWebSocketSession(Session session) {
        webSocketSessions.remove(session);
        pendingSnapshots.remove(session);
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException ex) {
                System.err.println("Unable to close WebSocket session: " + ex.getMessage());
            }
        }
    }

    private void handleWebSocketInput(Session session, String body) {
        try {
            applyInput(body);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            session.getAsyncRemote().sendText(
                    "{\"type\":\"error\",\"message\":\"Invalid movement update\"}",
                    result -> {
                        if (!result.isOK()) removeWebSocketSession(session);
                    });
        }
    }

    private void applyInput(String body) {
        boolean up = extractJsonBoolean(body, "up");
        boolean down = extractJsonBoolean(body, "down");
        boolean left = extractJsonBoolean(body, "left");
        boolean right = extractJsonBoolean(body, "right");
        boolean firing = extractJsonBoolean(body, "firing");
        double aimAngle = extractJsonDouble(body, "aimAngle", 0.0);
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
    }

    private abstract class JsonServlet extends HttpServlet {
        protected String readBody(HttpServletRequest request) throws IOException {
            try (InputStream is = request.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                return baos.toString(StandardCharsets.UTF_8.name());
            }
        }

        protected void sendJson(HttpServletResponse response, int status, String json)
                throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setStatus(status);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        }
    }

    private class HealthServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            response.setContentType("text/plain; charset=UTF-8");
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
        }
    }

    private class StaticHtmlServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            File file;
            if ("/Screenshot_2026-11-25_at_6.07.46_PM-removebg-preview.png"
                    .equals(request.getRequestURI())) {
                file = new File("Screenshot_2026-11-25_at_6.07.46_PM-removebg-preview.png");
                if (!file.exists()) {
                    file = new File("../Screenshot_2026-11-25_at_6.07.46_PM-removebg-preview.png");
                }
                if (!file.exists()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Missing player sprite image");
                    return;
                }
                response.setContentType("image/png");
            } else {
                file = new File("web/index.html");
                if (!file.exists()) file = new File("../web/index.html");
                if (!file.exists()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Missing web/index.html");
                    return;
                }
                response.setContentType("text/html; charset=UTF-8");
            }
            byte[] bytes = readAllBytes(file);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        }
    }

    private class StateServlet extends JsonServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            sendJson(response, HttpServletResponse.SC_OK, engine.toJsonSnapshot());
        }
    }

    private class StartServlet extends JsonServlet {
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            String body = readBody(request);
            String difficulty = extractJsonString(body, "difficulty", "Normal");
            String callsign = extractJsonString(body, "callsign", "Monkey D. Luffy");
            engine.resetGame(difficulty, callsign);
            sendJson(response, HttpServletResponse.SC_OK, engine.toJsonSnapshot());
        }
    }

    private class InputServlet extends JsonServlet {
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            try {
                applyInput(readBody(request));
                sendJson(response, HttpServletResponse.SC_OK, "{\"ok\":true}");
            } catch (IllegalArgumentException | ArithmeticException ex) {
                sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"error\":\"Invalid movement update\"}");
            }
        }
    }

    private class ActionServlet extends JsonServlet {
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            String body = readBody(request);
            String action = extractJsonString(body, "action", "");
            int param = (int) Math.round(extractJsonDouble(body, "param", 0.0));
            double aimAngle = extractJsonDouble(body, "aimAngle", Double.NaN);
            String result = engine.executePlayerAction(action, param, aimAngle);
            sendJson(response, HttpServletResponse.SC_OK, "{\"result\":\"" + result + "\"}");
        }
    }

    public static class GameWebSocketEndpoint extends Endpoint {
        private final GameServer gameServer;

        private GameWebSocketEndpoint(GameServer gameServer) {
            this.gameServer = gameServer;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            gameServer.addWebSocketSession(session);
            session.addMessageHandler(String.class,
                    (MessageHandler.Whole<String>) message ->
                            gameServer.handleWebSocketInput(session, message));
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            gameServer.webSocketSessions.remove(session);
            gameServer.pendingSnapshots.remove(session);
        }

        @Override
        public void onError(Session session, Throwable error) {
            System.err.println("WebSocket error: " + error.getMessage());
            gameServer.removeWebSocketSession(session);
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

    private byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
}
