package com.tissue.feature.workflow.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StateCategory {
    INITIAL,
    ACTIVE,
    COMPLETED;

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isInitial() {
        return this == INITIAL;
    }

    public boolean isNotInitial() {
        return !isInitial();
    }
}
