package com.tissue.workflow.domain.exception;

import static com.tissue.workflow.domain.exception.WorkflowErrorCode.*;

import java.util.Collection;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.common.exception.base.InternalServerException;
import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.workflow.domain.guard.GuardType;

public class WorkflowExceptions {

	private WorkflowExceptions() {
	}

	public static ResourceNotFoundException notFound(Long workflowId) {
		return new ResourceNotFoundException(WORKFLOW_NOT_FOUND)
			.addContext("workflowId", workflowId);
	}

	public static ResourceNotFoundException notFound(Long workflowId, String projectKey) {
		return new ResourceNotFoundException(WORKFLOW_NOT_FOUND)
			.addContext("workflowId", workflowId)
			.addContext("projectKey", projectKey);
	}

	public static ResourceNotFoundException stateNotFound(Long stateId, Long workflowId) {
		return new ResourceNotFoundException(WORKFLOW_STATE_NOT_FOUND)
			.addContext("stateId", stateId)
			.addContext("workflowId", workflowId);
	}

	public static ResourceNotFoundException transitionNotFound(Long transitionId, Long workflowId) {
		return new ResourceNotFoundException(WORKFLOW_TRANSITION_NOT_FOUND)
			.addContext("transitionId", transitionId)
			.addContext("workflowId", workflowId);
	}

	public static InternalServerException autoTransitionTargetNotFound(String issueKey, String currentStateName,
		String targetTransitionName) {
		return new InternalServerException(AUTO_TRANSITION_TARGET_NOT_FOUND)
			.addContext("issueKey", issueKey)
			.addContext("currentState", currentStateName)
			.addContext("targetTransition", targetTransitionName);
	}

	public static BadRequestException deadEndState(Collection<String> deadEndLabels) {
		return new BadRequestException(DEAD_END_STATE,
			"The following 'IN_PROGRESS' states have no outgoing transitions: %s. Please connect them to a next state or change their category to 'DONE'."
				.formatted(deadEndLabels))
			.addContext("deadEndStates", deadEndLabels);
	}

	public static ResourceConflictException duplicateGuardType(GuardType guardType) {
		return new ResourceConflictException(DUPLICATE_GUARD_TYPE)
			.addContext("guardType", guardType);
	}

	public static ResourceConflictException duplicateStateName(String stateName, String workflowName, Long workflowId) {
		return new ResourceConflictException(DUPLICATE_STATE_NAME)
			.addContext("stateName", stateName)
			.addContext("workflowName", workflowName)
			.addContext("workflowId", workflowId);
	}

	public static ResourceConflictException duplicateTransitionName(String transitionName, String sourceStateName,
		String workflowName, Long workflowId) {
		return new ResourceConflictException(DUPLICATE_TRANSITION_NAME)
			.addContext("transitionName", transitionName)
			.addContext("sourceStateName", sourceStateName)
			.addContext("workflowName", workflowName)
			.addContext("workflowId", workflowId);
	}

	public static ResourceConflictException duplicateWorkflowName(String workflowName, String projectKey,
		String workspaceKey) {
		return new ResourceConflictException(DUPLICATE_WORKFLOW_NAME)
			.addContext("workflowName", workflowName)
			.addContext("projectKey", projectKey)
			.addContext("workspaceKey", workspaceKey);
	}

	public static BadRequestException invalidTodoStateCount(int foundCount) {
		return new BadRequestException(INVALID_TODO_STATE_COUNT)
			.addContext("foundCount", foundCount);
	}

	public static BadRequestException invalidTransitionTarget(Collection<String> sourceStateNames,
		String targetStateName) {
		return new BadRequestException(INVALID_TRANSITION_TARGET)
			.addContext("invalidSourceStates", sourceStateNames)
			.addContext("targetState", targetStateName);
	}

	public static BadRequestException missingDoneState() {
		return new BadRequestException(MISSING_DONE_STATE);
	}

	public static BadRequestException orphanState(Collection<String> orphanStates, String initialLabel) {
		return new BadRequestException(ORPHAN_STATE,
			"Unreachable states detected: %s. All states must be reachable from '%s'."
				.formatted(orphanStates, initialLabel))
			.addContext("orphanStates", orphanStates)
			.addContext("initialState", initialLabel);
	}

	public static BadRequestException transitionGuardFailed(GuardType guardType, String reason, String issueKey,
		String workspaceKey) {
		return new BadRequestException(TRANSITION_GUARD_FAILED,
			"%s evaluation failed: %s".formatted(guardType, reason))
			.addContext("failedGuardType", guardType)
			.addContext("failureReason", reason)
			.addContext("issueKey", issueKey)
			.addContext("workspaceKey", workspaceKey);
	}

	public static BadRequestException workflowStateInUse(String stateNames) {
		return new BadRequestException(WORKFLOW_STATE_IN_USE)
			.addContext("activeStates", stateNames);
	}

	public static BadRequestException duplicateTransitionEdge(String sourceState, String targetState) {
		return new BadRequestException(DUPLICATE_TRANSITION_EDGE)
			.addContext("sourceState", sourceState)
			.addContext("targetState", targetState);
	}

	public static BadRequestException cannotDeleteInitialState(String stateName) {
		return new BadRequestException(CANNOT_DELETE_INITIAL_STATE)
			.addContext("stateName", stateName);
	}

	public static BadRequestException initialStateBelongMismatch() {
		return new BadRequestException(INITIAL_STATE_BELONG_MISMATCH);
	}

	public static BadRequestException initialStateCategoryMismatch() {
		return new BadRequestException(INITIAL_STATE_CATEGORY_MISMATCH);
	}
}
