package com.tissue.feature.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemRole {
    SUPER_ADMIN(1),
    ADMIN(2),
    USER(3);

    private final int level;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }

    public boolean isEqualOrHigherThan(SystemRole other) {
        return this.level <= other.level;
    }

    public boolean isHigherThan(SystemRole other) {
        return this.level < other.level;
    }
}
