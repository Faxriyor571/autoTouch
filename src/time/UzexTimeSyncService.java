package time;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UzexTimeSyncService {

    private static final URI HOME_URI = URI.create("https://spot.uzex.uz/");
    private static final URI NEGOTIATE_URI =
            URI.create("https://spot.uzex.uz/hub/timer/negotiate?negotiateVersion=1");
    private static final ZoneId UZEX_ZONE = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter SERVER_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS dd.MM.yyyy");
    private static final Pattern CLOCK_PATTERN = Pattern.compile(
            "id=\\\"clock-label\\\"[^>]*>\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\d{2}\\.\\d{2}\\.\\d{4})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\\"connectionToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );
    private static final Pattern HUB_TIME_PATTERN = Pattern.compile(
            "\\\"target\\\"\\s*:\\s*\\\"time-now\\\".*?"
                    + "\\\"arguments\\\"\\s*:\\s*\\[\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final int MAX_SAMPLES = 40;

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(3, runnable -> {
                Thread thread = new Thread(runnable, "uzex-time-sync");
                thread.setDaemon(true);
                return thread;
            });
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .executor(executor)
            .build();
    private final Object sampleLock = new Object();
    private final ArrayDeque<TimeSample> samples = new ArrayDeque<>();
    private final ArrayDeque<LatencySample> latencySamples = new ArrayDeque<>();
    private final List<Consumer<TimeSyncSnapshot>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();

    private volatile TimeSyncSnapshot snapshot = TimeSyncSnapshot.calibrating();
    private volatile LocalDate serverDate;
    private volatile WebSocket webSocket;
    private volatile boolean running;
    private volatile boolean hubConnected;
    private volatile long lastHubMillisOfDay = -1;

    public TimeSyncSnapshot getSnapshot() {
        return snapshot;
    }

    public synchronized void start(Consumer<TimeSyncSnapshot> listener) {
        if (listener != null) listeners.add(listener);
        publish(snapshot);
        if (running) return;

        running = true;
        executor.execute(this::initialCalibration);
        executor.scheduleWithFixedDelay(
                this::safeHttpSample,
                120,
                120,
                TimeUnit.SECONDS
        );
        executor.scheduleWithFixedDelay(
                this::safeLatencySample,
                10,
                10,
                TimeUnit.SECONDS
        );
    }

    private void initialCalibration() {
        for (int i = 0; i < 10 && running; i++) {
            safeHttpSample();
            if (i < 9) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        if (running) connectHub();
    }

    private void safeHttpSample() {
        if (!running) return;
        try {
            TimeSample sample = requestHttpSample();
            synchronized (sampleLock) {
                samples.addLast(sample);
                latencySamples.addLast(new LatencySample(
                        sample.rttNanos(),
                        sample.capturedAt()
                ));
                while (samples.size() > MAX_SAMPLES) samples.removeFirst();
                while (latencySamples.size() > MAX_SAMPLES) latencySamples.removeFirst();
                Instant cutoff = Instant.now().minusSeconds(300);
                while (!samples.isEmpty()
                        && samples.peekFirst().capturedAt().isBefore(cutoff)) {
                    samples.removeFirst();
                }
                while (!latencySamples.isEmpty()
                        && latencySamples.peekFirst().capturedAt().isBefore(cutoff)) {
                    latencySamples.removeFirst();
                }
            }
            serverDate = TimeSyncSnapshot.fromEpochNanos(sample.serverEpochNanos())
                    .atZone(UZEX_ZONE)
                    .toLocalDate();
            recalculateSnapshot(null);
        } catch (Exception e) {
            TimeSyncSnapshot current = snapshot;
            if (current.sampleCount() == 0) {
                snapshot = new TimeSyncSnapshot(
                        SyncStatus.DISCONNECTED,
                        current.anchorNanoTime(),
                        current.serverEpochNanosAtAnchor(),
                        current.offsetMillis(),
                        current.minRttMillis(),
                        current.medianRttMillis(),
                        current.jitterMillis(),
                        current.uncertaintyMillis(),
                        0,
                        false,
                        Instant.now(),
                        "UZEX vaqtiga ulanib bo'lmadi: " + e.getMessage()
                );
                publish(snapshot);
            }
        }
    }

    private void safeLatencySample() {
        if (!running) return;
        try {
            LatencySample sample = requestLatencySample();
            synchronized (sampleLock) {
                latencySamples.addLast(sample);
                while (latencySamples.size() > MAX_SAMPLES) latencySamples.removeFirst();
                Instant cutoff = Instant.now().minusSeconds(300);
                while (!latencySamples.isEmpty()
                        && latencySamples.peekFirst().capturedAt().isBefore(cutoff)) {
                    latencySamples.removeFirst();
                }
            }
            recalculateSnapshot(null);
        } catch (Exception ignored) {
            // SignalR va full calibration ishlashda davom etadi.
        }
    }

    private TimeSample requestHttpSample() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(HOME_URI)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "AutoTouch-TimeSync/1.0")
                .GET()
                .build();

        long startNano = System.nanoTime();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        long endNano = System.nanoTime();

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        Matcher matcher = CLOCK_PATTERN.matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("clock-label topilmadi");
        }

        LocalDateTime serverTime = LocalDateTime.parse(matcher.group(1), SERVER_FORMAT);
        long serverEpochNanos = TimeSyncSnapshot.toEpochNanos(
                serverTime.atZone(UZEX_ZONE).toInstant()
        );
        long rttNanos = endNano - startNano;
        long midpointNano = startNano + rttNanos / 2;
        return new TimeSample(midpointNano, serverEpochNanos, rttNanos, Instant.now());
    }

    private LatencySample requestLatencySample() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(HOME_URI)
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "AutoTouch-TimeSync/1.0")
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        long startNano = System.nanoTime();
        HttpResponse<Void> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding()
        );
        long endNano = System.nanoTime();
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new IllegalStateException("HEAD HTTP " + response.statusCode());
        }
        return new LatencySample(endNano - startNano, Instant.now());
    }

    private void recalculateSnapshot(String messageOverride) {
        List<TimeSample> all;
        List<LatencySample> latency;
        synchronized (sampleLock) {
            all = new ArrayList<>(samples);
            latency = new ArrayList<>(latencySamples);
        }
        if (all.isEmpty() || latency.isEmpty()) return;

        all.sort(Comparator.comparingLong(TimeSample::rttNanos));
        int bestCount = Math.min(all.size(), Math.max(3, (int) Math.ceil(all.size() * 0.3)));
        List<TimeSample> best = all.subList(0, bestCount);
        long anchorNano = System.nanoTime();

        long[] serverPredictions = best.stream()
                .mapToLong(sample -> sample.serverEpochNanos()
                        + (anchorNano - sample.midpointNano()))
                .sorted()
                .toArray();
        long serverAtAnchor = median(serverPredictions);

        double[] allRttMs = latency.stream()
                .mapToDouble(sample -> sample.rttNanos() / 1_000_000.0)
                .sorted()
                .toArray();
        int bestLatencyCount = Math.min(
                allRttMs.length,
                Math.max(3, (int) Math.ceil(allRttMs.length * 0.3))
        );
        double[] bestRttMs = java.util.Arrays.copyOf(allRttMs, bestLatencyCount);
        double minRtt = allRttMs[0];
        double medianRtt = median(allRttMs);
        // TLS warm-up va vaqtinchalik stall'lar jitter sifatida hisoblanmaydi.
        // Critical timing uchun eng tez 30% yo'lning tarqalishi muhim.
        double jitter = standardDeviation(bestRttMs);
        double uncertainty = minRtt / 2.0 + jitter;

        long localEpochNow = TimeSyncSnapshot.toEpochNanos(Instant.now());
        double offsetMs = (serverAtAnchor - localEpochNow) / 1_000_000.0;
        SyncStatus status = all.size() >= 5 && jitter <= 30
                ? SyncStatus.SYNCED
                : SyncStatus.DEGRADED;

        snapshot = new TimeSyncSnapshot(
                status,
                anchorNano,
                serverAtAnchor,
                offsetMs,
                minRtt,
                medianRtt,
                jitter,
                uncertainty,
                all.size(),
                hubConnected,
                Instant.now(),
                messageOverride != null
                        ? messageOverride
                        : status == SyncStatus.SYNCED
                        ? "UZEX vaqti sinxronlangan"
                        : "Ko'proq o'lchov kutilmoqda"
        );
        publish(snapshot);
    }

    private void connectHub() {
        if (!running) return;
        try {
            HttpRequest negotiateRequest = HttpRequest.newBuilder(NEGOTIATE_URI)
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "text/plain;charset=UTF-8")
                    .header("User-Agent", "AutoTouch-TimeSync/1.0")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(
                    negotiateRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            Matcher tokenMatcher = TOKEN_PATTERN.matcher(response.body());
            if (!tokenMatcher.find()) {
                throw new IllegalStateException("SignalR token topilmadi");
            }

            String token = URLEncoder.encode(tokenMatcher.group(1), StandardCharsets.UTF_8);
            URI socketUri = URI.create("wss://spot.uzex.uz/hub/timer?id=" + token);
            httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .buildAsync(socketUri, new HubListener())
                    .thenAccept(socket -> webSocket = socket)
                    .exceptionally(error -> {
                        scheduleReconnect();
                        return null;
                    });
        } catch (Exception e) {
            hubConnected = false;
            recalculateSnapshot("SignalR ulanmagan, HTTP sync ishlayapti");
            scheduleReconnect();
        }
    }

    private void onHubTime(long ticks) {
        LocalDate date = serverDate;
        TimeSyncSnapshot current = snapshot;
        if (date == null || current.sampleCount() == 0) return;

        long millisOfDay = ticks / 10_000L;
        long previousMillisOfDay = lastHubMillisOfDay;
        if (previousMillisOfDay >= 0
                && previousMillisOfDay - millisOfDay > 12 * 60 * 60 * 1_000L) {
            date = date.plusDays(1);
            serverDate = date;
        }
        lastHubMillisOfDay = millisOfDay;
        long serverSentEpochNanos = TimeSyncSnapshot.toEpochNanos(
                date.atStartOfDay(UZEX_ZONE).toInstant()
        ) + millisOfDay * 1_000_000L;
        long receiveNano = System.nanoTime();
        long estimatedInboundNanos = current.estimatedOutboundLatencyNanos();
        long serverAtReceive = serverSentEpochNanos + estimatedInboundNanos;
        long localEpochNow = TimeSyncSnapshot.toEpochNanos(Instant.now());

        snapshot = new TimeSyncSnapshot(
                current.status(),
                receiveNano,
                serverAtReceive,
                (serverAtReceive - localEpochNow) / 1_000_000.0,
                current.minRttMillis(),
                current.medianRttMillis(),
                current.jitterMillis(),
                current.uncertaintyMillis(),
                current.sampleCount(),
                true,
                Instant.now(),
                "UZEX SignalR online"
        );
        publish(snapshot);
    }

    private void scheduleReconnect() {
        if (!running || !reconnectScheduled.compareAndSet(false, true)) return;
        executor.schedule(() -> {
            reconnectScheduled.set(false);
            connectHub();
        }, 5, TimeUnit.SECONDS);
    }

    private void publish(TimeSyncSnapshot value) {
        for (Consumer<TimeSyncSnapshot> listener : listeners) {
            try {
                listener.accept(value);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public synchronized void shutdown() {
        running = false;
        WebSocket socket = webSocket;
        if (socket != null) socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        executor.shutdownNow();
    }

    private static long median(long[] values) {
        int middle = values.length / 2;
        if (values.length % 2 == 1) return values[middle];
        return values[middle - 1] + (values[middle] - values[middle - 1]) / 2;
    }

    private static double median(double[] values) {
        int middle = values.length / 2;
        if (values.length % 2 == 1) return values[middle];
        return (values[middle - 1] + values[middle]) / 2.0;
    }

    private static double standardDeviation(double[] values) {
        if (values.length < 2) return 0;
        double average = 0;
        for (double value : values) average += value;
        average /= values.length;
        double sum = 0;
        for (double value : values) {
            double delta = value - average;
            sum += delta * delta;
        }
        return Math.sqrt(sum / values.length);
    }

    private record TimeSample(
            long midpointNano,
            long serverEpochNanos,
            long rttNanos,
            Instant capturedAt
    ) {}

    private record LatencySample(long rttNanos, Instant capturedAt) {}

    private final class HubListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket socket) {
            hubConnected = true;
            reconnectScheduled.set(false);
            socket.request(1);
            socket.sendText("{\"protocol\":\"json\",\"version\":1}\u001e", true);
            recalculateSnapshot("UZEX SignalR ulanmoqda");
        }

        @Override
        public CompletionStage<?> onText(WebSocket socket,
                                         CharSequence data,
                                         boolean last) {
            synchronized (buffer) {
                buffer.append(data);
                int separator;
                while ((separator = buffer.indexOf("\u001e")) >= 0) {
                    String message = buffer.substring(0, separator);
                    buffer.delete(0, separator + 1);
                    Matcher matcher = HUB_TIME_PATTERN.matcher(message);
                    if (matcher.find()) {
                        onHubTime(Long.parseLong(matcher.group(1)));
                    }
                }
            }
            socket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket,
                                          int statusCode,
                                          String reason) {
            hubConnected = false;
            recalculateSnapshot("SignalR uzildi, HTTP sync ishlayapti");
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            hubConnected = false;
            recalculateSnapshot("SignalR xatosi, HTTP sync ishlayapti");
            scheduleReconnect();
        }
    }
}
