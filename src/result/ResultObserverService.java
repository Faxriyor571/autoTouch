package result;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ResultObserverService {

    public static final int PORT = 17_321;
    private static final int MAX_EVENT_BYTES = 8_192;

    private final String sessionToken;
    private final CopyOnWriteArrayList<Consumer<ResultObservation>> listeners =
            new CopyOnWriteArrayList<>();
    private volatile String lastClockText = "";
    private volatile Instant lastClockObservedAt;
    private HttpServer server;
    private ExecutorService observerExecutor;
    private volatile Instant lastExtensionSeen;

    public ResultObserverService() {
        byte[] token = new byte[32];
        new SecureRandom().nextBytes(token);
        sessionToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public synchronized void start(Consumer<ResultObservation> listener) {
        if (listener != null) listeners.add(listener);
        if (server != null) return;
        try {
            server = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", PORT),
                    16
            );
            server.createContext("/session", this::handleSession);
            server.createContext("/event", this::handleEvent);
            server.createContext("/clock", this::handleClock);
            observerExecutor = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "result-observer-http");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(observerExecutor);
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Result observer portini ochib bo'lmadi: " + PORT,
                    e
            );
        }
    }

    public boolean isExtensionOnline() {
        Instant seen = lastExtensionSeen;
        return seen != null && seen.plusSeconds(25).isAfter(Instant.now());
    }

    public Instant getLastExtensionSeen() {
        return lastExtensionSeen;
    }

    public String getLastClockText() {
        return lastClockText;
    }

    public Instant getLastClockObservedAt() {
        return lastClockObservedAt;
    }

    private void handleSession(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                || !isAllowedOrigin(exchange)) {
            send(exchange, 403, "forbidden");
            return;
        }
        lastExtensionSeen = Instant.now();
        send(exchange, 200, sessionToken);
    }

    private void handleEvent(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())
                || !isAllowedOrigin(exchange)) {
            send(exchange, 403, "forbidden");
            return;
        }

        String providedToken = exchange.getRequestHeaders()
                .getFirst("X-AutoTouch-Session");
        if (providedToken == null || !MessageDigest.isEqual(
                providedToken.getBytes(StandardCharsets.UTF_8),
                sessionToken.getBytes(StandardCharsets.UTF_8))) {
            send(exchange, 401, "unauthorized");
            return;
        }

        byte[] body = exchange.getRequestBody().readNBytes(MAX_EVENT_BYTES + 1);
        if (body.length > MAX_EVENT_BYTES) {
            send(exchange, 413, "too large");
            return;
        }

        Map<String, String> form = parseForm(new String(body, StandardCharsets.UTF_8));
        ResultObservation observation = new ResultObservation(
                clean(form.get("source"), 20),
                clean(form.get("method"), 10).toUpperCase(),
                cleanPath(form.get("path")),
                parseInt(form.get("status")),
                parseDouble(form.get("durationMs")),
                instantFromMillis(form.get("observedAtEpochMs")),
                clean(form.get("candidateKey"), 160),
                clean(form.get("candidateValue"), 160),
                epochNanosFromMillis(form.get("candidateEpochMs")),
                parseInt(form.get("candidateScore"))
        );
        lastExtensionSeen = Instant.now();
        for (Consumer<ResultObservation> listener : listeners) {
            try {
                listener.accept(observation);
            } catch (RuntimeException ignored) {
            }
        }
        send(exchange, 204, "");
    }

    private void handleClock(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())
                || !isAllowedOrigin(exchange)) {
            send(exchange, 403, "forbidden");
            return;
        }

        String providedToken = exchange.getRequestHeaders()
                .getFirst("X-AutoTouch-Session");
        if (providedToken == null || !MessageDigest.isEqual(
                providedToken.getBytes(StandardCharsets.UTF_8),
                sessionToken.getBytes(StandardCharsets.UTF_8))) {
            send(exchange, 401, "unauthorized");
            return;
        }

        byte[] body = exchange.getRequestBody().readNBytes(MAX_EVENT_BYTES + 1);
        if (body.length > MAX_EVENT_BYTES) {
            send(exchange, 413, "too large");
            return;
        }

        Map<String, String> form = parseForm(new String(body, StandardCharsets.UTF_8));
        lastClockText = clean(form.get("text"), 64);
        lastClockObservedAt = instantFromMillis(form.get("observedAtEpochMs"));
        lastExtensionSeen = Instant.now();
        send(exchange, 204, "");
    }

    private boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        String origin = allowedOrigin(exchange);
        if (origin != null) {
            Headers headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", origin);
            headers.set("Vary", "Origin");
            headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.set(
                    "Access-Control-Allow-Headers",
                    "Content-Type, X-AutoTouch-Session"
            );
            headers.set("Access-Control-Max-Age", "600");
        }
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(origin == null ? 403 : 204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private boolean isAllowedOrigin(HttpExchange exchange) {
        return allowedOrigin(exchange) != null;
    }

    private String allowedOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null) return null;
        if (origin.startsWith("chrome-extension://")
                || origin.startsWith("moz-extension://")
                || origin.equals("https://spot.uzex.uz")) {
            return origin;
        }
        return null;
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        String origin = allowedOrigin(exchange);
        if (origin != null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        for (String part : body.split("&")) {
            int equals = part.indexOf('=');
            String key = equals >= 0 ? part.substring(0, equals) : part;
            String value = equals >= 0 ? part.substring(equals + 1) : "";
            values.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            );
        }
        return values;
    }

    private static String clean(String value, int maxLength) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\r\\n\\u0000]", "").trim();
        return cleaned.substring(0, Math.min(cleaned.length(), maxLength));
    }

    private static String cleanPath(String value) {
        String path = clean(value, 300);
        return path.startsWith("/") ? path : "/unknown";
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static Instant instantFromMillis(String value) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(value));
        } catch (Exception ignored) {
            return Instant.now();
        }
    }

    private static long epochNanosFromMillis(String value) {
        try {
            return Math.multiplyExact(Long.parseLong(value), 1_000_000L);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public synchronized void shutdown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (observerExecutor != null) {
            observerExecutor.shutdownNow();
            observerExecutor = null;
        }
    }
}
