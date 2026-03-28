package com.tissue.feature.notification.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public record NotificationMessage(
        @Column(name = "message_data", columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON)
        Map<String, String> data) {

    public NotificationMessage() {
        this(new HashMap<>());
    }
}
