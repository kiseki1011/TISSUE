package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;

public class StateMigrationRequiredException extends BadRequestException {

    public record Detail(Long stateId, String stateName, long activeIssueCount) {}

    public StateMigrationRequiredException(List<Detail> statesRequiringMigration) {
        super(WorkflowErrorCode.STATE_MIGRATION_REQUIRED);
        addContext("statesRequiringMigration", statesRequiringMigration);
    }
}
