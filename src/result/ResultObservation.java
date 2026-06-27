package result;

import java.time.Instant;

public record ResultObservation(
        String source,
        String method,
        String path,
        int statusCode,
        double durationMillis,
        Instant observedAt,
        String candidateKey,
        String candidateValue,
        long candidateServerEpochNanos,
        int candidateScore
) {
    public boolean hasServerTimestamp() {
        return candidateServerEpochNanos > 0 && candidateScore >= 2;
    }

    public String profileKey() {
        String normalizedKey = candidateKey == null
                ? ""
                : candidateKey.replaceAll("\\[\\d+]", "[]");
        return method + " " + path + " | " + normalizedKey;
    }
}
