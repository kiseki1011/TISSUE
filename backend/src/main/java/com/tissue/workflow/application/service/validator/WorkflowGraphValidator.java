package com.tissue.workflow.application.service.validator;

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
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workflow.domain.exception.WorkflowExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowGraphValidator {

	public void ensureValidWorkflowGraph(Workflow wf) {
		ensureSingleToDo(wf);
		ensureAtLeastOneDone(wf);
		ensureNoIncomingToToDo(wf);
		ensureNoOrphans(wf);
		ensureValidInProgressFlow(wf);
	}

	private void ensureSingleToDo(Workflow wf) {
		List<WorkflowState> todoStates = wf.getStatesByCategory(StateCategory.TODO);

		if (todoStates.size() != 1) {
			throw WorkflowExceptions.invalidTodoStateCount(todoStates.size());
		}

		WorkflowState todoState = todoStates.get(0);
		if (!todoState.equals(wf.getInitialState())) {
			throw new IllegalStateException("Initial state pointer mismatch.");
		}
	}

	private void ensureAtLeastOneDone(Workflow wf) {
		boolean doneNotExist = wf.getStatesByCategory(StateCategory.DONE).isEmpty();
		if (doneNotExist) {
			throw WorkflowExceptions.missingDoneState();
		}
	}

	private void ensureNoIncomingToToDo(Workflow wf) {
		WorkflowState initialState = wf.getInitialState();

		List<WorkflowTransition> invalidTransitions = wf.getTransitions().stream()
			.filter(t -> t.getTargetState().equals(initialState))
			.toList();

		if (!invalidTransitions.isEmpty()) {
			List<String> sourceNames = invalidTransitions.stream()
				.map(t -> t.getSourceState().getDisplayLabel())
				.toList();

			throw WorkflowExceptions.invalidTransitionTarget(sourceNames, initialState.getDisplayLabel());
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

		// BFS로 initial에서 시작해 도달 가능한 상태를 모두 방문
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
			.map(WorkflowState::getDisplayLabel)
			.toList();

		if (!orphanStates.isEmpty()) {
			throw WorkflowExceptions.orphanState(orphanStates, initial.getDisplayLabel());
		}
	}

	private void ensureValidInProgressFlow(Workflow wf) {
		List<WorkflowTransition> activeTransitions = wf.getTransitions();

		Set<WorkflowState> statesWithOutgoing = activeTransitions.stream()
			.map(WorkflowTransition::getSourceState) // 객체 자체를 수집
			.collect(Collectors.toSet());

		List<String> deadEnds = wf.getStatesByCategory(StateCategory.IN_PROGRESS).stream()
			.filter(state -> !statesWithOutgoing.contains(state))
			.map(WorkflowState::getDisplayLabel)
			.toList();

		if (!deadEnds.isEmpty()) {
			throw WorkflowExceptions.deadEndState(deadEnds);
		}
	}

	// TODO: 어차피 ensureSingleToDo에서 보장이 되고, initial 여부도 확인 될텐데 굳이 필요한가?
	//  중복 검증이지 않을까?
	private WorkflowState ensureInitialExists(Workflow wf) {
		WorkflowState state = wf.getInitialState();
		if (state == null || state.isArchived()) {
			throw new IllegalStateException("Initial must exist and be active.");
		}
		return state;
	}
}
