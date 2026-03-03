package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkflowErrorCode implements ErrorCode {
    WORKFLOW_NOT_FOUND("Workflow not found"),
    WORKFLOW_STATE_NOT_FOUND("Workflow state not found"),
    WORKFLOW_TRANSITION_NOT_FOUND("Workflow transition not found"),
    AUTO_TRANSITION_TARGET_NOT_FOUND("Configured auto-transition target not found in current state"),
    DEAD_END_STATE("The following 'ACTIVE' states have no outgoing transitions"),
    DUPLICATE_GUARD_TYPE("Duplicate guard type detected for this transition"),
    DUPLICATE_STATE_NAME("A state with this name already exists in the workflow"),
    DUPLICATE_TRANSITION_NAME("A transition with this name and source state already exists in the workflow"),
    DUPLICATE_WORKFLOW_NAME("Workflow with this label already exists"),
    INVALID_INITIAL_STATE_COUNT("Workflow must have exactly one 'INITIAL' state"),
    INVALID_TRANSITION_TARGET("Transitions cannot target the 'INITIAL' state"),
    MISSING_COMPLETED_STATE("Workflow must have at least one 'COMPLETED' state"),
    ORPHAN_STATE("Unreachable states detected"),
    TRANSITION_GUARD_FAILED("Transition guard evaluation failed"),
    WORKFLOW_STATE_IN_USE("Cannot delete workflow states that are currently assigned to active issues"),
    DUPLICATE_TRANSITION_EDGE("Duplicate transition between these two states already exists"),
    CANNOT_DELETE_INITIAL_STATE("Cannot delete the state categorized as 'INITIAL' in a workflow"),
    INITIAL_STATE_BELONG_MISMATCH("State must belong to the workflow"),
    INITIAL_STATE_CATEGORY_MISMATCH("Initial (first) state must be categorized as 'INITIAL'"),
    INVALID_GUARD_PARAMETER("Invalid parameter for transition guard"),
    GUARD_NOT_FOUND("Required transition guard not found in the system"),
    INVALID_GRAPH_REQUEST("Invalid workflow graph request format"),
    WORKFLOW_VERSION_MISMATCH("Workflow has been modified by another user");

    private final String defaultMessage;
}
