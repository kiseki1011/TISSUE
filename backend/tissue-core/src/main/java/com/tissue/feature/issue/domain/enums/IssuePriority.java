package com.tissue.feature.issue.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(
        description = "Issue priority level, from highest to lowest: "
                + "P0 (blocker), "
                + "P1 (critical), "
                + "P2 (major), "
                + "P3 (minor), "
                + "P4 (trivial)")
@Getter
@RequiredArgsConstructor
public enum IssuePriority {
    P0(0),
    P1(1),
    P2(2),
    P3(3),
    P4(4);

    private final int level;
}
