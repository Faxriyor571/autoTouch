package result;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AdaptiveLatencyModel {

    private static final Path MODEL_PATH = Path.of("data", "latency-model.properties");
    private static final int MAX_ERRORS_PER_PROFILE = 20;
    private static final double MAX_ACCEPTED_ERROR_MS = 10_000;
    private static final double MAX_CORRECTION_MS = 250;

    private final Map<String, ArrayDeque<Double>> errorsByProfile = new HashMap<>();
    private final CopyOnWriteArrayList<Consumer<AdaptiveSnapshot>> listeners =
            new CopyOnWriteArrayList<>();
    private final Set<String> observedProfilesThisAttempt = new HashSet<>();

    private long targetServerEpochNanos;
    private Instant armedAt;
    private Instant expiresAt;
    private AdaptiveSnapshot snapshot = AdaptiveSnapshot.empty();

    public AdaptiveLatencyModel() {
        load();
        recalculate("Saqlangan adaptive model yuklandi");
    }

    public synchronized void addListener(Consumer<AdaptiveSnapshot> listener) {
        if (listener != null) {
            listeners.add(listener);
            listener.accept(snapshot);
        }
    }

    public synchronized void arm(long targetServerEpochNanos) {
        this.targetServerEpochNanos = targetServerEpochNanos;
        this.armedAt = Instant.now();
        this.expiresAt = Instant.ofEpochSecond(
                Math.floorDiv(targetServerEpochNanos, 1_000_000_000L),
                Math.floorMod(targetServerEpochNanos, 1_000_000_000L)
        ).plusSeconds(30);
        observedProfilesThisAttempt.clear();
    }

    public synchronized boolean observe(ResultObservation observation) {
        if (armedAt == null || observation == null || !observation.hasServerTimestamp()) {
            return false;
        }
        // Browser Date.now() millisecond resolutionda; Java Instant nanosecond
        // resolutionda. 1s tolerans faqat transport timestamp rounding uchun.
        if (observation.observedAt().plusSeconds(1).isBefore(armedAt)
                || observation.observedAt().isAfter(expiresAt)) {
            return false;
        }
        if (observation.statusCode() < 200 || observation.statusCode() >= 400) {
            return false;
        }

        String profile = observation.profileKey();
        if (!observedProfilesThisAttempt.add(profile)) return false;
        double errorMs =
                (observation.candidateServerEpochNanos() - targetServerEpochNanos)
                        / 1_000_000.0;
        if (!Double.isFinite(errorMs) || Math.abs(errorMs) > MAX_ACCEPTED_ERROR_MS) {
            return false;
        }

        ArrayDeque<Double> errors = errorsByProfile.computeIfAbsent(
                profile,
                ignored -> new ArrayDeque<>()
        );
        errors.addLast(errorMs);
        while (errors.size() > MAX_ERRORS_PER_PROFILE) errors.removeFirst();
        recalculate(String.format("Real UZEX xatosi: %+.3f ms", errorMs));
        save();
        return true;
    }

    public synchronized long correctionNanos() {
        return Math.round(snapshot.correctionMillis() * 1_000_000.0);
    }

    public synchronized AdaptiveSnapshot getSnapshot() {
        return snapshot;
    }

    private void recalculate(String message) {
        ProfileStats strictBest = errorsByProfile.entrySet().stream()
                .map(entry -> stats(entry.getKey(), entry.getValue()))
                .filter(stats -> stats.sampleCount() >= 3 && stats.madMillis() <= 100)
                .max(Comparator
                        .comparingInt(ProfileStats::sampleCount)
                        .thenComparingDouble(stats -> -stats.madMillis()))
                .orElse(null);

        ProfileStats fallbackBest = errorsByProfile.entrySet().stream()
                .map(entry -> stats(entry.getKey(), entry.getValue()))
                .filter(stats -> stats.sampleCount() >= 3)
                .max(Comparator
                        .comparingInt(ProfileStats::sampleCount)
                        .thenComparingDouble(stats -> -stats.madMillis()))
                .orElse(null);

        ProfileStats chosen = strictBest != null ? strictBest : fallbackBest;
        if (chosen == null) {
            snapshot = new AdaptiveSnapshot(
                    0,
                    0,
                    "",
                    0,
                    message == null ? "1 ta real natija kutilmoqda" : message
            );
        } else {
            double correction = Math.max(
                    -MAX_CORRECTION_MS,
                    Math.min(MAX_CORRECTION_MS, chosen.medianMillis())
            );
            snapshot = new AdaptiveSnapshot(
                    correction,
                    chosen.sampleCount(),
                    chosen.profile(),
                    chosen.madMillis(),
                    message == null
                            ? "Adaptive correction faol"
                            : message
            );
        }
        for (Consumer<AdaptiveSnapshot> listener : listeners) {
            try {
                listener.accept(snapshot);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static ProfileStats stats(String profile, ArrayDeque<Double> errors) {
        List<Double> sorted = new ArrayList<>(errors);
        sorted.sort(Double::compareTo);
        double median = median(sorted);
        List<Double> deviations = sorted.stream()
                .map(value -> Math.abs(value - median))
                .sorted()
                .toList();
        return new ProfileStats(
                profile,
                median,
                median(deviations),
                sorted.size()
        );
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) return 0;
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) return values.get(middle);
        return (values.get(middle - 1) + values.get(middle)) / 2.0;
    }

    private void load() {
        if (!Files.isRegularFile(MODEL_PATH)) return;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(MODEL_PATH)) {
            properties.load(input);
            for (String encodedProfile : properties.stringPropertyNames()) {
                String profile = new String(
                        Base64.getUrlDecoder().decode(encodedProfile),
                        java.nio.charset.StandardCharsets.UTF_8
                );
                ArrayDeque<Double> errors = new ArrayDeque<>();
                for (String token : properties.getProperty(encodedProfile, "").split(",")) {
                    if (!token.isBlank()) errors.add(Double.parseDouble(token));
                }
                while (errors.size() > MAX_ERRORS_PER_PROFILE) errors.removeFirst();
                if (!errors.isEmpty()) errorsByProfile.put(profile, errors);
            }
        } catch (Exception ignored) {
            errorsByProfile.clear();
        }
    }

    private void save() {
        try {
            Files.createDirectories(MODEL_PATH.getParent());
            Properties properties = new Properties();
            for (Map.Entry<String, ArrayDeque<Double>> entry : errorsByProfile.entrySet()) {
                String encodedProfile = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8)
                );
                String values = entry.getValue().stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(","));
                properties.setProperty(encodedProfile, values);
            }
            Path temporary = MODEL_PATH.resolveSibling(MODEL_PATH.getFileName() + ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "AutoTouch adaptive latency model");
            }
            try {
                Files.move(
                        temporary,
                        MODEL_PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (Exception unsupportedAtomicMove) {
                Files.move(temporary, MODEL_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }

    public record AdaptiveSnapshot(
            double correctionMillis,
            int sampleCount,
            String profile,
            double madMillis,
            String message
    ) {
        public static AdaptiveSnapshot empty() {
            return new AdaptiveSnapshot(0, 0, "", 0, "3 ta real natija kutilmoqda");
        }

        public boolean active() {
            return sampleCount >= 3 && !profile.isBlank();
        }
    }

    private record ProfileStats(
            String profile,
            double medianMillis,
            double madMillis,
            int sampleCount
    ) {}
}
