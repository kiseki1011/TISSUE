package com.tissue.notification.domain.vo;

import com.tissue.common.jpa.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public record NotificationMessage(
        // title/content are derived from NotificationType and args during display
        @Column(columnDefinition = "TEXT") @Convert(converter = StringListConverter.class)
        List<String> args) {

    public NotificationMessage() {
        this(new ArrayList<>());
    }
}
