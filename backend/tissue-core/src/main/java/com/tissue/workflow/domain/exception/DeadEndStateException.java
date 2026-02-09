package com.tissue.workflow.domain.exception;

import com.tissue.exception.base.BadRequestException;
import java.util.Collection;

public class DeadEndStateException extends BadRequestException {

    public DeadEndStateException(Collection<String> deadEndStateNames) {
        super(
                WorkflowErrorCode.DEAD_END_STATE,
                ("The following 'ACTIVE' states have no outgoing transitions: %s."
                                + " Please connect them to a next state or change their category to 'COMPLETED'.")
                        .formatted(deadEndStateNames));
        addContext("deadEndStates", deadEndStateNames);
    }
}
