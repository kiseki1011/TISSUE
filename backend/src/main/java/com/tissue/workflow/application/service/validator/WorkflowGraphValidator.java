package com.tissue.workflow.application.service.validator;

import static com.tissue.workflow.domain.enums.StateCategory.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.exception.WorkflowExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowGraphValidator {

	public void ensureValidWorkflowGraph(Workflow wf) {
		ensureSingleInitial(wf);
		ensureAtLeastOneCompleted(wf);
		ensureNoIncomingToInitial(wf);
		ensureNoOrphans(wf);
		ensureValidActiveFlow(wf);
	}

	private void ensureSingleInitial(Workflow wf) {
		List<WorkflowState> initialStates = wf.getStatesByCategory(INITIAL);

		if (initialStates.size() != 1) {
			throw WorkflowExceptions.invalidInitialStateCount(initialStates.size());
		}

		WorkflowState initialState = initialStates.get(0);
		if (!initialState.equals(wf.getInitialState())) {
			throw new IllegalStateException("Initial state pointer mismatch");
		}
	}

	private void ensureAtLeastOneCompleted(Workflow wf) {
		boolean completedNotExist = wf.getStatesByCategory(COMPLETED).isEmpty();
		if (completedNotExist) {
			throw WorkflowExceptions.missingCompletedState();
		}
	}

	private void ensureNoIncomingToInitial(Workflow wf) {
		WorkflowState initialState = wf.getInitialState();

		List<WorkflowTransition> invalidTransitions = wf.getTransitions().stream()
			.filter(t -> t.getTargetState().equals(initialState))
			.toList();

		if (!invalidTransitions.isEmpty()) {
			List<String> sourceNames = invalidTransitions.stream()
				.map(t -> t.getSourceState().getDisplayName())
				.toList();

			throw WorkflowExceptions.invalidTransitionTarget(sourceNames, initialState.getDisplayName());
		}
	}

	private void ensureNoOrphans(Workflow wf) {
		WorkflowState initial = ensureInitialExists(wf);

		Map<WorkflowState, List<WorkflowState>> reachableFrom = new HashMap<>();
		for (var transition : wf.getTransitions()) {
			if (transition.isArchived()) {
				continue;
			}

			WorkflowState from = transition.getSourceState();
			WorkflowState to = transition.getTargetState();

			reachableFrom.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
		}

		// BFS
		Set<WorkflowState> reachableStates = new HashSet<>();
		Deque<WorkflowState> toVisit = new ArrayDeque<>();
		toVisit.add(initial);
		reachableStates.add(initial);

		while (!toVisit.isEmpty()) {
			WorkflowState current = toVisit.poll();
			List<WorkflowState> nextStatuses = reachableFrom.getOrDefault(current, List.of());

			for (WorkflowState next : nextStatuses) {
				if (reachableStates.add(next)) {
					toVisit.add(next);
				}
			}
		}

		List<String> orphanStates = wf.getActiveStates().stream()
			.filter(s -> !reachableStates.contains(s))
			.map(WorkflowState::getDisplayName)
			.toList();

		if (!orphanStates.isEmpty()) {
			throw WorkflowExceptions.orphanState(orphanStates, initial.getDisplayName());
		}
	}

	private void ensureValidActiveFlow(Workflow wf) {
		List<WorkflowTransition> activeTransitions = wf.getTransitions();

		Set<WorkflowState> statesWithOutgoing = activeTransitions.stream()
			.map(WorkflowTransition::getSourceState) // 객체 자체를 수집
			.collect(Collectors.toSet());

		List<String> deadEnds = wf.getStatesByCategory(ACTIVE).stream()
			.filter(state -> !statesWithOutgoing.contains(state))
			.map(WorkflowState::getDisplayName)
			.toList();

		if (!deadEnds.isEmpty()) {
			throw WorkflowExceptions.deadEndState(deadEnds);
		}
	}

	// TODO: is this really needed? doesnt ensureSingleInitial already validates this?
	private WorkflowState ensureInitialExists(Workflow wf) {
		WorkflowState state = wf.getInitialState();
		if (state == null || state.isArchived()) {
			throw new IllegalStateException("Initial must exist and be active");
		}
		return state;
	}
}
