package com.tissue.feature.notification.domain;

import com.tissue.shared.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class FailedEmail extends BaseDateEntity {

    @Column(nullable = false)
    private Long notificationId;

    @Column(nullable = false)
    private String receiverEmail;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant nextRetryAt;

    @SuppressWarnings("NullAway.Init")
    protected FailedEmail() {}

    @Builder
    public FailedEmail(
            Long notificationId, String receiverEmail, String subject, String body, @Nullable String errorMessage) {
        this.notificationId = notificationId;
        this.receiverEmail = receiverEmail;
        this.subject = subject;
        this.body = body;
        this.errorMessage = errorMessage;
        this.retryCount = 0;
        this.nextRetryAt = Instant.now().plus(Duration.ofMinutes(1));
    }

    public void incrementRetryCount() {
        this.retryCount++;
        long minutesToWait = (long) Math.pow(2, retryCount);
        this.nextRetryAt = Instant.now().plus(Duration.ofMinutes(minutesToWait));
    }
}
