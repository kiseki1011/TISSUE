package com.tissue.api.issue.workflow.domain.service;

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
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;
import com.tissue.api.issue.workflow.domain.model.WorkflowTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowGraphValidator {

	public record StatusValidationData(String ref, boolean initial, boolean terminal) {
	}

	public record TransitionValidationData(String sourceRef, String targetRef) {
	}

	public void validateWorkflowGraphStructure(
		List<StatusValidationData> statusDataList,
		List<TransitionValidationData> transitionDataList
	) {
		ensureExactlyOneInitial(statusDataList);
		ensureAtLeastOneTerminal(statusDataList);
		ensureTransitionReferencesValid(statusDataList, transitionDataList);
		ensureNoSelfLoops(transitionDataList);
	}

	public void ensureValidWorkflowGraph(Workflow wf) {
		ensureNoIncomingToInitial(wf.getInitialStatus(), wf.getTransitions());
		ensureNoOrphans(wf);
	}

	public void ensureNotDeletingInitial(
		Set<WorkflowStatus> toDelete,
		WorkflowStatus initial
	) {
		if (toDelete.contains(initial)) {
			throw new InvalidOperationException("Cannot delete the initial status.");
		}
	}

	private void ensureExactlyOneInitial(List<StatusValidationData> statusDataList) {
		long initialCount = statusDataList.stream().filter(StatusValidationData::initial).count();
		if (initialCount != 1) {
			throw new InvalidOperationException("Exactly one initial required.");
		}
	}

	private void ensureAtLeastOneTerminal(List<StatusValidationData> statusDataList) {
		long count = statusDataList.stream().filter(StatusValidationData::terminal).count();
		if (count == 0) {
			throw new InvalidOperationException("At least one terminal required.");
		}
	}

	// transition이 가리키는 key들이 실제 status 키 집합 안에 존재하는지 확인
	private void ensureTransitionReferencesValid(
		List<StatusValidationData> statusDataList,
		List<TransitionValidationData> transitionDataList
	) {
		Set<String> refs = statusDataList.stream()
			.map(StatusValidationData::ref)
			.collect(Collectors.toSet());

		if (refs.size() != statusDataList.size()) {
			throw new InvalidOperationException("Duplicate status keys found.");
		}

		for (var t : transitionDataList) {
			if (!refs.contains(t.sourceRef())) {
				throw new InvalidOperationException("Unknown source reference: " + t.sourceRef());
			}
			if (!refs.contains(t.targetRef())) {
				throw new InvalidOperationException("Unknown target reference: " + t.targetRef());
			}
		}
	}

	private void ensureNoSelfLoops(List<TransitionValidationData> transitionDataList) {
		for (var t : transitionDataList) {
			if (Objects.equals(t.sourceRef(), t.targetRef())) {
				throw new InvalidOperationException("Self-loop not allowed.");
			}
		}
	}

	private void ensureNoIncomingToInitial(
		WorkflowStatus initial,
		Collection<WorkflowTransition> allTransitions
	) {
		for (var t : allTransitions) {
			if (t.getTargetStatus().equals(initial)) {
				throw new InvalidOperationException("Transitions into the initial status are not allowed.");
			}
		}
	}

	private WorkflowStatus ensureInitialExists(Workflow wf) {
		WorkflowStatus status = wf.getInitialStatus();
		if (status == null || status.isArchived()) {
			throw new InvalidOperationException("Initial must exist and be active.");
		}
		return status;
	}

	private void ensureNoOrphans(Workflow wf) {
		WorkflowStatus initial = ensureInitialExists(wf);

		// 인접 리스트(그래프)를 만든다
		Map<WorkflowStatus, List<WorkflowStatus>> reachableFrom = new HashMap<>();
		for (var transition : wf.getTransitions()) {
			if (transition.isArchived()) {
				continue;
			}

			WorkflowStatus from = transition.getSourceStatus();
			WorkflowStatus to = transition.getTargetStatus();

			reachableFrom.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
		}

		// BFS로 initial에서 시작해 도달 가능한 상태를 모두 방문
		Set<WorkflowStatus> reachableStatuses = new HashSet<>();
		Deque<WorkflowStatus> toVisit = new ArrayDeque<>();
		toVisit.add(initial);
		reachableStatuses.add(initial);

		while (!toVisit.isEmpty()) {
			WorkflowStatus current = toVisit.poll();
			List<WorkflowStatus> nextStatuses = reachableFrom.getOrDefault(current, List.of());

			for (WorkflowStatus next : nextStatuses) {
				if (reachableStatuses.add(next)) {
					toVisit.add(next);
				}
			}
		}

		// 살아있는 상태 수와 방문한 상태 수가 같아야 '고아 없음'
		long totalStatuses = wf.getStatuses().stream()
			.filter(s -> !s.isArchived())
			.count();

		if (reachableStatuses.size() != totalStatuses) {
			throw new InvalidOperationException("Orphan statuses exist (unreachable from initial).");
		}
	}
}
