package com.tissue.notification.domain;

import com.tissue.common.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedEmail extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private LocalDateTime nextRetryAt;

    @Builder
    public FailedEmail(
            Long notificationId, String receiverEmail, String subject, String body, @Nullable String errorMessage) {
        this.notificationId = notificationId;
        this.receiverEmail = receiverEmail;
        this.subject = subject;
        this.body = body;
        this.errorMessage = errorMessage;
        this.retryCount = 0;
        this.nextRetryAt = LocalDateTime.now().plusMinutes(1);
    }

    // Exponential backoff
    public void incrementRetryCount() {
        this.retryCount++;
        long minutesToWait = (long) Math.pow(2, retryCount);
        this.nextRetryAt = LocalDateTime.now().plusMinutes(minutesToWait);
    }
}
