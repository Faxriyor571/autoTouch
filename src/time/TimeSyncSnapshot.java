package time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record TimeSyncSnapshot(
        SyncStatus status,
        long anchorNanoTime,
        long serverEpochNanosAtAnchor,
        double offsetMillis,
        double minRttMillis,
        double medianRttMillis,
        double jitterMillis,
        double uncertaintyMillis,
        int sampleCount,
        boolean hubConnected,
        Instant updatedAt,
        String message
) {
    public static final ZoneId UZEX_ZONE = ZoneId.of("Asia/Tashkent");

    public static TimeSyncSnapshot calibrating() {
        return new TimeSyncSnapshot(
                SyncStatus.CALIBRATING,
                System.nanoTime(),
                0,
                0,
                0,
                0,
                0,
                Double.POSITIVE_INFINITY,
                0,
                false,
                Instant.now(),
                "UZEX vaqti o'lchanmoqda"
        );
    }

    public boolean isUsable() {
        if (status != SyncStatus.SYNCED && status != SyncStatus.DEGRADED) return false;
        return updatedAt != null && updatedAt.plusSeconds(60).isAfter(Instant.now());
    }

    public long serverEpochNanosNow() {
        if (!isUsable()) throw new IllegalStateException("UZEX vaqti sinxronlanmagan");
        return serverEpochNanosAtAnchor + (System.nanoTime() - anchorNanoTime);
    }

    public ZonedDateTime serverNow() {
        return fromEpochNanos(serverEpochNanosNow()).atZone(UZEX_ZONE);
    }

    public ZonedDateTime conservativeServerNow() {
        long displayBiasNanos = conservativeDisplayBiasNanos();
        return fromEpochNanos(serverEpochNanosNow() - displayBiasNanos).atZone(UZEX_ZONE);
    }

    public long targetNanoTime(LocalTime targetTime, boolean compensateNetwork) {
        return targetNanoTime(
                targetServerEpochNanos(targetTime),
                compensateNetwork
        );
    }

    public long targetNanoTime(long targetServerEpochNanos,
                               boolean compensateNetwork) {
        long networkLeadNanos = compensateNetwork
                ? Math.round(minRttMillis * 500_000.0)
                : 0L;
        return anchorNanoTime
                + (targetServerEpochNanos - serverEpochNanosAtAnchor)
                - networkLeadNanos;
    }

    public long targetServerEpochNanos(LocalTime targetTime) {
        long serverNowNanos = serverEpochNanosNow();
        LocalDate serverDate = fromEpochNanos(serverNowNanos)
                .atZone(UZEX_ZONE)
                .toLocalDate();
        return toEpochNanos(
                serverDate.atTime(targetTime).atZone(UZEX_ZONE).toInstant()
        );
    }

    public long estimatedServerEpochNanos(long monotonicNanoTime) {
        return serverEpochNanosAtAnchor + (monotonicNanoTime - anchorNanoTime);
    }

    public long estimatedOutboundLatencyNanos() {
        return Math.round(minRttMillis * 500_000.0);
    }

    public double adaptiveBiasMillis() {
        double baseBiasMillis = (minRttMillis * 0.5) + (jitterMillis * 0.5);
        double guardMillis = Math.max(20.0, Math.min(120.0, uncertaintyMillis * 0.5));
        return Math.max(20.0, Math.min(180.0, baseBiasMillis + guardMillis));
    }

    private long conservativeDisplayBiasNanos() {
        double baseBiasMillis = (minRttMillis * 0.5) + (jitterMillis * 0.5);
        double guardMillis = Math.max(20.0, Math.min(120.0, uncertaintyMillis * 0.5));
        double totalBiasMillis = Math.max(20.0, Math.min(180.0, baseBiasMillis + guardMillis));
        return Math.round(totalBiasMillis * 1_000_000.0);
    }

    public static long toEpochNanos(Instant instant) {
        return Math.addExact(
                Math.multiplyExact(instant.getEpochSecond(), 1_000_000_000L),
                instant.getNano()
        );
    }

    public static Instant fromEpochNanos(long epochNanos) {
        long seconds = Math.floorDiv(epochNanos, 1_000_000_000L);
        long nanos = Math.floorMod(epochNanos, 1_000_000_000L);
        return Instant.ofEpochSecond(seconds, nanos);
    }
}



