package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;
import java.util.Collection;

public class OrphanStateException extends BadRequestException {

    public OrphanStateException(Collection<String> orphanStateNames, String initialStateName) {
        super(
                WorkflowErrorCode.ORPHAN_STATE,
                "Unreachable states detected: %s. All states must be reachable from '%s'."
                        .formatted(orphanStateNames, initialStateName));
        addContext("orphanStates", orphanStateNames);
        addContext("initialState", initialStateName);
    }
}
