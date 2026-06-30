package time;

import java.time.Instant;
import java.time.ZoneId;

public final class ClockMath {
    public static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Tashkent");

    private ClockMath() {
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
