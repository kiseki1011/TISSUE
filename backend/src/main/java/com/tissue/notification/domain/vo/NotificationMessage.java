package com.tissue.notification.domain.vo;

import com.tissue.common.jpa.converter.StringMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.HashMap;
import java.util.Map;

@Embeddable
public record NotificationMessage(
        @Column(name = "message_data", columnDefinition = "TEXT") @Convert(converter = StringMapConverter.class)
        Map<String, String> data) {

    public NotificationMessage() {
        this(new HashMap<>());
    }
}
