package com.tissue.api.workflow.domain.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.domain.model.WorkflowState;
import com.tissue.api.workflow.domain.model.WorkflowTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowGraphValidator {

	public record StateValidationData(String stateRef, boolean initial, boolean terminal) {
	}

	public record TransitionValidationData(String sourceStateRef, String targetStateRef) {
	}

	public void validateWorkflowGraphStructure(
		List<StateValidationData> stateValidations,
		List<TransitionValidationData> transitionValidations
	) {
		ensureExactlyOneInitial(stateValidations);
		ensureAtLeastOneTerminal(stateValidations);
		ensureTransitionReferencesValid(stateValidations, transitionValidations);
		ensureNoSelfLoops(transitionValidations);
	}

	public void ensureValidWorkflowGraph(Workflow wf) {
		ensureNoIncomingToInitial(wf.getInitialState(), wf.getTransitions());
		ensureNoOrphans(wf);
	}

	public void ensureNotDeletingInitial(
		Set<WorkflowState> toDelete,
		WorkflowState initial
	) {
		if (toDelete.contains(initial)) {
			throw new InvalidOperationException("Cannot delete the initial status.");
		}
	}

	private void ensureExactlyOneInitial(List<StateValidationData> stateValidations) {
		long initialCount = stateValidations.stream()
			.filter(StateValidationData::initial)
			.count();

		if (initialCount != 1) {
			throw new InvalidOperationException("Exactly one initial required.");
		}
	}

	private void ensureAtLeastOneTerminal(List<StateValidationData> stateValidations) {
		long count = stateValidations.stream()
			.filter(StateValidationData::terminal)
			.count();

		if (count == 0) {
			throw new InvalidOperationException("At least one terminal required.");
		}
	}

	// transition이 가리키는 key들이 실제 status 키 집합 안에 존재하는지 확인
	private void ensureTransitionReferencesValid(
		List<StateValidationData> stateValidations,
		List<TransitionValidationData> transitionValidations
	) {
		Set<String> refs = stateValidations.stream()
			.map(StateValidationData::stateRef)
			.collect(Collectors.toSet());

		if (refs.size() != stateValidations.size()) {
			throw new InvalidOperationException("Duplicate state keys found.");
		}

		for (var t : transitionValidations) {
			if (!refs.contains(t.sourceStateRef())) {
				throw new InvalidOperationException("Unknown source reference: " + t.sourceStateRef());
			}
			if (!refs.contains(t.targetStateRef())) {
				throw new InvalidOperationException("Unknown target reference: " + t.targetStateRef());
			}
		}
	}

	private void ensureNoSelfLoops(List<TransitionValidationData> transitionValidations) {
		for (var t : transitionValidations) {
			if (Objects.equals(t.sourceStateRef(), t.targetStateRef())) {
				throw new InvalidOperationException("Self-loop not allowed.");
			}
		}
	}

	private void ensureNoIncomingToInitial(
		WorkflowState initial,
		Collection<WorkflowTransition> allTransitions
	) {
		for (var t : allTransitions) {
			if (t.getTargetState().equals(initial)) {
				throw new InvalidOperationException("Transitions into the initial states are not allowed.");
			}
		}
	}

	private WorkflowState ensureInitialExists(Workflow wf) {
		WorkflowState state = wf.getInitialState();
		if (state == null || state.isArchived()) {
			throw new InvalidOperationException("Initial must exist and be active.");
		}
		return state;
	}

	private void ensureNoOrphans(Workflow wf) {
		WorkflowState initial = ensureInitialExists(wf);

		// 인접 리스트(그래프)를 만든다
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

		// 살아있는 상태 수와 방문한 상태 수가 같아야 '고아 없음'
		long totalStates = wf.getStates().stream()
			.filter(s -> !s.isArchived())
			.count();

		if (reachableStates.size() != totalStates) {
			throw new InvalidOperationException("Orphan states exist (unreachable from initial).");
		}
	}
}
