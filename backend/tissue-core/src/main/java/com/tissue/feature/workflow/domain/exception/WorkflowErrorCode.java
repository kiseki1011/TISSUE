package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkflowErrorCode implements ErrorCode {
    WORKFLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "Workflow not found"),
    WORKFLOW_STATE_NOT_FOUND(HttpStatus.NOT_FOUND, "Workflow state not found"),
    WORKFLOW_TRANSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "Workflow transition not found"),
    AUTO_TRANSITION_TARGET_NOT_FOUND(
            HttpStatus.INTERNAL_SERVER_ERROR, "Configured auto-transition target not found in current state"),
    DEAD_END_STATE(HttpStatus.BAD_REQUEST, "The following 'ACTIVE' states have no outgoing transitions"),
    DUPLICATE_GUARD_TYPE(HttpStatus.CONFLICT, "Duplicate guard type detected for this transition"),
    DUPLICATE_STATE_NAME(HttpStatus.CONFLICT, "A state with this name already exists in the workflow"),
    DUPLICATE_TRANSITION_NAME(
            HttpStatus.CONFLICT, "A transition with this name and source state already exists in the workflow"),
    DUPLICATE_WORKFLOW_NAME(HttpStatus.CONFLICT, "Workflow with this label already exists"),
    INVALID_INITIAL_STATE_COUNT(HttpStatus.BAD_REQUEST, "Workflow must have exactly one 'INITIAL' state"),
    INVALID_TRANSITION_TARGET(HttpStatus.BAD_REQUEST, "Transitions cannot target the 'INITIAL' state"),
    MISSING_COMPLETED_STATE(HttpStatus.BAD_REQUEST, "Workflow must have at least one 'COMPLETED' state"),
    ORPHAN_STATE(HttpStatus.BAD_REQUEST, "Unreachable states detected"),
    TRANSITION_BLOCKED_BY_DEPENDENCY(HttpStatus.BAD_REQUEST, "This issue is blocked by unresolved issues"),
    ASSIGNEE_REQUIRED(HttpStatus.BAD_REQUEST, "An assignee is required before this transition"),
    UNRESOLVED_CHILD_ISSUES(HttpStatus.BAD_REQUEST, "This issue has unresolved child issues"),
    LINKED_BRANCH_REQUIRED(HttpStatus.BAD_REQUEST, "A linked VCS branch is required before this transition"),
    CHANGE_REQUEST_BLOCKED(HttpStatus.BAD_REQUEST, "Transition is blocked by requested changes"),
    INSUFFICIENT_APPROVALS(HttpStatus.BAD_REQUEST, "Required number of approvals has not been met"),
    WORKFLOW_STATE_IN_USE(
            HttpStatus.CONFLICT, "Cannot delete workflow states that are currently assigned to active issues"),
    DUPLICATE_TRANSITION_EDGE(HttpStatus.BAD_REQUEST, "Duplicate transition between these two states already exists"),
    CANNOT_DELETE_INITIAL_STATE(
            HttpStatus.BAD_REQUEST, "Cannot delete the state categorized as 'INITIAL' in a workflow"),
    INITIAL_STATE_BELONG_MISMATCH(HttpStatus.BAD_REQUEST, "State must belong to the workflow"),
    INITIAL_STATE_CATEGORY_MISMATCH(HttpStatus.BAD_REQUEST, "Initial (first) state must be categorized as 'INITIAL'"),
    INVALID_GUARD_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid parameter for transition guard"),
    GUARD_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Required transition guard not found in the system"),
    MISSING_NODE_IDENTIFIER(
            HttpStatus.BAD_REQUEST,
            "Either an existing ID or a temporary key must be provided for node identification"),
    INCOMPLETE_NEW_STATE(HttpStatus.BAD_REQUEST, "New states require 'name' and 'color'"),
    INCOMPLETE_NEW_TRANSITION(HttpStatus.BAD_REQUEST, "New transitions require 'name'"),
    MIGRATION_TARGET_BEING_DELETED(
            HttpStatus.BAD_REQUEST, "Cannot migrate issues to a state that is also being deleted"),
    TEMP_KEY_NOT_RESOLVED(HttpStatus.BAD_REQUEST, "Referenced temporary key does not match any defined state"),
    STATE_MIGRATION_REQUIRED(
            HttpStatus.BAD_REQUEST, "Active issues exist in states being deleted; migration mapping required"),
    WORKFLOW_VERSION_MISMATCH(HttpStatus.CONFLICT, "Workflow has been modified by another user");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
