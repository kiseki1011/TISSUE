package com.tissue.feature.workflow.domain.enums;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StateCategory {
    INITIAL,
    ACTIVE,
    COMPLETED,
    ABORTED;

    private static final Set<StateCategory> TERMINAL = Set.of(COMPLETED, ABORTED);

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isInitial() {
        return this == INITIAL;
    }

    public boolean isAborted() {
        return this == ABORTED;
    }

    public boolean isNotInitial() {
        return !isInitial();
    }

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public static Set<StateCategory> terminalCategories() {
        return TERMINAL;
    }
}
