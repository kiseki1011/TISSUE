package com.tissue.feature.authentication.domain;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh"),
    REGISTER("register");

    private final String value;

    public static TokenType from(String value) {
        return Arrays.stream(values())
                .filter(t -> t.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid token type: " + value));
    }
}
