package org.marionette.controlplane.usecases.inbound;

import java.time.Instant;

public class AbnTestResult {
    private final boolean success;
    private final Instant completedAt;
    private final String message;

    public AbnTestResult() {
        this.success = true;
        this.completedAt = Instant.now();
        this.message = "A/B test completed successfully";
    }

    public AbnTestResult(boolean success, String message) {
        this.success = success;
        this.completedAt = Instant.now();
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getMessage() {
        return message;
    }
}
