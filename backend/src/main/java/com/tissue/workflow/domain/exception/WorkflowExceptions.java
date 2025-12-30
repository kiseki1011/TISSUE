package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.CURRENT_STATE;
import static com.tissue.global.exception.ContextKeys.GUARD_TYPE;
import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.REASON;
import static com.tissue.global.exception.ContextKeys.STATE;
import static com.tissue.global.exception.ContextKeys.STATE_ID;
import static com.tissue.global.exception.ContextKeys.TRANSITION;
import static com.tissue.global.exception.ContextKeys.TRANSITION_ID;
import static com.tissue.global.exception.ContextKeys.WORKFLOW;
import static com.tissue.global.exception.ContextKeys.WORKFLOW_ID;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.AUTO_TRANSITION_TARGET_NOT_FOUND;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.CANNOT_DELETE_INITIAL_STATE;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.DEAD_END_STATE;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_GUARD_TYPE;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_STATE_NAME;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_TRANSITION_EDGE;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_TRANSITION_NAME;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.INITIAL_STATE_BELONG_MISMATCH;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.INITIAL_STATE_CATEGORY_MISMATCH;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.INVALID_GRAPH_REQUEST;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.INVALID_GUARD_PARAMETER;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.INVALID_INITIAL_STATE_COUNT;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.INVALID_TRANSITION_TARGET;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.MISSING_COMPLETED_STATE;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.ORPHAN_STATE;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.TRANSITION_GUARD_FAILED;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_NOT_FOUND;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_STATE_IN_USE;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_STATE_NOT_FOUND;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND;
import static com.tissue.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_VERSION_MISMATCH;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.InternalServerException;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.workflow.domain.guard.GuardType;
import java.util.Collection;

public class WorkflowExceptions {

    private WorkflowExceptions() {}

    public static ResourceNotFoundException notFound(Long workflowId) {
        return new ResourceNotFoundException(WORKFLOW_NOT_FOUND).addContext(WORKFLOW_ID, workflowId);
    }

    public static ResourceNotFoundException notFound(Long workflowId, String projectKey) {
        return new ResourceNotFoundException(WORKFLOW_NOT_FOUND)
                .addContext(WORKFLOW_ID, workflowId)
                .addContext(PROJECT_KEY, projectKey);
    }

    public static ResourceNotFoundException stateNotFound(Long stateId, Long workflowId) {
        return new ResourceNotFoundException(WORKFLOW_STATE_NOT_FOUND)
                .addContext(STATE_ID, stateId)
                .addContext(WORKFLOW_ID, workflowId);
    }

    public static ResourceNotFoundException transitionNotFound(Long transitionId, Long workflowId) {
        return new ResourceNotFoundException(WORKFLOW_TRANSITION_NOT_FOUND)
                .addContext(TRANSITION_ID, transitionId)
                .addContext(WORKFLOW_ID, workflowId);
    }

    public static InternalServerException autoTransitionTargetNotFound(
            String issueKey, String currentStateName, String targetTransitionName) {
        return new InternalServerException(AUTO_TRANSITION_TARGET_NOT_FOUND)
                .addContext(ISSUE_KEY, issueKey)
                .addContext(CURRENT_STATE, currentStateName)
                .addContext("targetTransition", targetTransitionName);
    }

    public static BadRequestException deadEndState(Collection<String> deadEndStateNames) {
        String detailMessage = ("The following 'ACTIVE' states have no outgoing transitions: %s. Please connect"
                        + " them to a next state or change their category to 'COMPLETED'.")
                .formatted(deadEndStateNames);

        return new BadRequestException(DEAD_END_STATE, detailMessage).addContext("deadEndStates", deadEndStateNames);
    }

    public static ResourceConflictException duplicateGuardType(GuardType guardType) {
        return new ResourceConflictException(DUPLICATE_GUARD_TYPE).addContext(GUARD_TYPE, guardType);
    }

    public static ResourceConflictException duplicateStateName(String stateName, String workflowName, Long workflowId) {
        return new ResourceConflictException(DUPLICATE_STATE_NAME)
                .addContext(STATE, stateName)
                .addContext(WORKFLOW, workflowName)
                .addContext(WORKFLOW_ID, workflowId);
    }

    public static ResourceConflictException duplicateTransitionName(
            String transitionName, String sourceStateName, String workflowName, Long workflowId) {
        return new ResourceConflictException(DUPLICATE_TRANSITION_NAME)
                .addContext(TRANSITION, transitionName)
                .addContext("sourceStateName", sourceStateName)
                .addContext(WORKFLOW, workflowName)
                .addContext(WORKFLOW_ID, workflowId);
    }

    public static ResourceConflictException duplicateWorkflowName(
            String workflowName, String projectKey, String workspaceKey) {
        return new ResourceConflictException(DUPLICATE_WORKFLOW_NAME)
                .addContext(WORKFLOW, workflowName)
                .addContext(PROJECT_KEY, projectKey)
                .addContext(WORKSPACE_KEY, workspaceKey);
    }

    public static BadRequestException invalidInitialStateCount(int foundCount) {
        return new BadRequestException(INVALID_INITIAL_STATE_COUNT).addContext("foundCount", foundCount);
    }

    public static BadRequestException invalidTransitionTarget(
            Collection<String> sourceStateNames, String targetStateName) {
        return new BadRequestException(INVALID_TRANSITION_TARGET)
                .addContext("invalidSourceStates", sourceStateNames)
                .addContext("targetState", targetStateName);
    }

    public static BadRequestException missingCompletedState() {
        return new BadRequestException(MISSING_COMPLETED_STATE);
    }

    public static BadRequestException orphanState(Collection<String> orphanStateNames, String initialStateName) {
        String detailMessage = "Unreachable states detected: %s. All states must be reachable from '%s'."
                .formatted(orphanStateNames, initialStateName);

        return new BadRequestException(ORPHAN_STATE, detailMessage)
                .addContext("orphanStates", orphanStateNames)
                .addContext("initialState", initialStateName);
    }

    public static BadRequestException transitionGuardFailed(
            GuardType guardType, String reason, String issueKey, String workspaceKey) {
        String detailMessage = "%s evaluation failed. Reason: %s.".formatted(guardType, reason);

        return new BadRequestException(TRANSITION_GUARD_FAILED, detailMessage)
                .addContext(GUARD_TYPE, guardType)
                .addContext(REASON, reason)
                .addContext(ISSUE_KEY, issueKey)
                .addContext(WORKSPACE_KEY, workspaceKey);
    }

    public static BadRequestException workflowStateInUse(String stateNames) {
        return new BadRequestException(WORKFLOW_STATE_IN_USE).addContext("activeStates", stateNames);
    }

    public static BadRequestException duplicateTransitionEdge(String sourceStateName, String targetStateName) {
        return new BadRequestException(DUPLICATE_TRANSITION_EDGE)
                .addContext("sourceState", sourceStateName)
                .addContext("targetState", targetStateName);
    }

    public static BadRequestException cannotDeleteInitialState(Long workflowId, String workflowName, String stateName) {
        return new BadRequestException(CANNOT_DELETE_INITIAL_STATE)
                .addContext(WORKFLOW_ID, workflowId)
                .addContext(WORKFLOW, workflowName)
                .addContext(STATE, stateName);
    }

    public static BadRequestException initialStateBelongMismatch() {
        return new BadRequestException(INITIAL_STATE_BELONG_MISMATCH);
    }

    public static BadRequestException initialStateCategoryMismatch() {
        return new BadRequestException(INITIAL_STATE_CATEGORY_MISMATCH);
    }

    public static BadRequestException invalidGuardParameter(String reason, GuardType guardType) {
        return new BadRequestException(INVALID_GUARD_PARAMETER, reason)
                .addContext(GUARD_TYPE, guardType)
                .addContext(REASON, reason);
    }

    public static BadRequestException invalidGraphRequest(String detail, String target, String errorType) {
        return new BadRequestException(INVALID_GRAPH_REQUEST, detail)
                .addContext("target", target)
                .addContext("errorType", errorType);
    }

    public static ResourceConflictException versionMismatch(Long clientVersion, Long currentVersion) {
        String detail = "Workflow Client version: %d, Current version: %d".formatted(clientVersion, currentVersion);
        return new ResourceConflictException(WORKFLOW_VERSION_MISMATCH, detail)
                .addContext("clientVersion", clientVersion)
                .addContext("currentVersion", currentVersion);
    }
}
