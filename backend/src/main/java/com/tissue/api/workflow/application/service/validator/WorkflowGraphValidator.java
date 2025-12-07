package com.tissue.api.workflow.application.service.validator;

import static com.tissue.api.issue.domain.enums.StateCategory.*;

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

import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;

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
			// TODO: 커스텀 예외 추가 vs IllegalStateException
			throw new RuntimeException("Workflow must have exactly a single 'TODO' state.");
		}

		WorkflowState todoState = todoStates.get(0);
		if (!todoState.equals(wf.getInitialState())) {
			throw new IllegalStateException("Internal Error: Initial state pointer mismatch.");
		}
	}

	private void ensureAtLeastOneDone(Workflow wf) {
		boolean doneNotExist = wf.getStatesByCategory(DONE).isEmpty();
		if (doneNotExist) {
			// TODO: 커스텀 예외 추가 vs IllegalStateException
			throw new RuntimeException("Workflow must have at least one 'DONE' state.");
		}
	}

	private void ensureNoIncomingToToDo(Workflow wf) {
		WorkflowState todoState = wf.getInitialState();

		boolean hasIncoming = wf.getTransitions().stream()
			.anyMatch(t -> t.getTargetState().equals(todoState));

		if (hasIncoming) {
			// TODO: 커스텀 예외 추가 vs IllegalStateException
			throw new RuntimeException("Transitions that point to a TODO state are not allowed.");
		}
	}

	private WorkflowState ensureInitialExists(Workflow wf) {
		WorkflowState state = wf.getInitialState();
		if (state == null || state.isArchived()) {
			// TODO: 커스텀 예외 추가 vs IllegalStateException
			throw new RuntimeException("Initial must exist and be active.");
		}
		return state;
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

		// 살아있는 상태 수와 방문한 상태 수가 같아야 고아 없음
		long totalStates = wf.getStates().stream()
			.filter(s -> !s.isArchived())
			.count();

		if (reachableStates.size() != totalStates) {
			// TODO: 커스텀 예외 추가 vs IllegalStateException
			throw new RuntimeException("Orphan states exist (unreachable from initial).");
		}
	}

	private void ensureValidInProgressFlow(Workflow wf) {
		List<WorkflowTransition> activeTransitions = wf.getTransitions();

		Set<WorkflowState> statesWithOutgoing = activeTransitions.stream()
			.map(WorkflowTransition::getSourceState) // 객체 자체를 수집
			.collect(Collectors.toSet());

		List<String> deadEnds = wf.getStatesByCategory(IN_PROGRESS).stream()
			.filter(state -> !statesWithOutgoing.contains(state))
			.map(WorkflowState::getDisplayLabel)
			.toList();

		if (!deadEnds.isEmpty()) {
			// TODO: 커스텀 예외 추가 vs IllegalStateException
			throw new RuntimeException(
				"The following 'IN_PROGRESS' states have no outgoing transitions (Dead Ends): "
					+ deadEnds + ". Please connect them to a next state or change their category to 'DONE'."
			);
		}
	}

}
