package com.tissue.feature.issue.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

@Schema(
        description = "Relation type between issues. Direction is from source to target: "
                + "RELEVANT (bidirectional, informational link), "
                + "BLOCKS (source blocks target), "
                + "CAUSES (source causes target), "
                + "DUPLICATES (source is a duplicate of target)")
@RequiredArgsConstructor
public enum IssueRelationType {
    RELEVANT(false),
    BLOCKS(true),
    CAUSES(true),
    DUPLICATES(true);

    private final boolean requiresAcyclicCheck;

    public boolean requiresAcyclicCheck() {
        return this.requiresAcyclicCheck;
    }
}
