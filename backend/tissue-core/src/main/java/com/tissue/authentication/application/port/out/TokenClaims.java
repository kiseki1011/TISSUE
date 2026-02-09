package com.tissue.authentication.application.port.out;

import java.util.Map;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record TokenClaims(
        String subject,
        Long memberId,
        String provider,
        String identifier,
        String email,
        boolean elevated,
        Map<String, Object> attributes) {

    @Nullable
    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }
}
