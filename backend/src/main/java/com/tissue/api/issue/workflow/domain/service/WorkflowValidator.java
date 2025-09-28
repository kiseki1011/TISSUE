package com.tissue.api.issue.workflow.domain.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;
import com.tissue.api.issue.workflow.domain.model.WorkflowTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	// TODO: blank 관련 검증은 bean validation을 적용하면 굳이 할 필요 있을까? 과한 방어적 프로그래밍 같은데
	public void validateCommand(CreateWorkflowCommand cmd) {
		Set<Label> labels = new HashSet<>();
		int initial = 0;
		int terminal = 0;
		for (var sc : cmd.statuses()) {
			if (!labels.add(sc.label())) {
				throw new DuplicateResourceException("Duplicate step label: " + sc.label());
			}
			if (sc.initial()) {
				initial++;
			}
			if (sc.terminal()) {
				terminal++;
			}
		}
		if (initial != 1) {
			throw new InvalidOperationException("Workflow must have a single initial step.");
		}
		if (terminal == 0) {
			throw new InvalidOperationException("Workflow must have at least one terminal step.");
		}

		Set<String> keys = new HashSet<>();
		for (var sc : cmd.statuses()) {
			if (sc.tempKey() == null || sc.tempKey().isBlank()) {
				throw new InvalidOperationException("Status tempKey must not be blank.");
			}
			if (!keys.add(sc.tempKey())) {
				throw new DuplicateResourceException("Duplicate status tempKey: " + sc.tempKey());
			}
		}

		for (var tc : cmd.transitions()) {
			if (tc.sourceTempKey() == null || tc.sourceTempKey().isBlank()) {
				throw new InvalidOperationException("Transition sourceTempKey must not be blank.");
			}
			if (tc.targetTempKey() == null || tc.targetTempKey().isBlank()) {
				throw new InvalidOperationException("Transition targetTempKey must not be blank.");
			}
			if (!keys.contains(tc.sourceTempKey())) {
				throw new InvalidOperationException("Unknown source status tempKey: " + tc.sourceTempKey());
			}
			if (!keys.contains(tc.targetTempKey())) {
				throw new InvalidOperationException("Unknown target status tempKey: " + tc.targetTempKey());
			}
			if (tc.sourceTempKey().equals(tc.targetTempKey())) {
				throw new InvalidOperationException("Self-loop not allowed.");
			}
		}
	}

	/**
	 * 메인 플로우가 '초기 → ... → (종료)'로 이어지는 단일 직선 경로임을 보장한다.
	 * 조건 불만족 시 예외를 던진다.
	 * 경로가 비어 있으면 안됨(최소 하나의 transition으로 이루어져야 함)
	 */
	public void ensureMainFlowSingleLine(Workflow wf, List<WorkflowTransition> mainFlow) {
		if (mainFlow.isEmpty()) {
			throw new InvalidOperationException("Main flow of transitions must not be empty.");
		}

		WorkflowStatus initialStatus = wf.getInitialStatus();
		if (initialStatus == null) {
			throw new InvalidOperationException("Initial status must be set before validating main flow.");
		}

		Set<WorkflowStatus> statusSet = new HashSet<>(wf.getStatuses());
		for (var t : mainFlow) {
			if (!statusSet.contains(t.getSourceStatus()) || !statusSet.contains(t.getTargetStatus())) {
				throw new InvalidOperationException("Main flow must connect statuses of this workflow.");
			}
		}

		Map<WorkflowStatus, Integer> outCountByStatus = new IdentityHashMap<>();
		Map<WorkflowStatus, Integer> inCountByStatus = new IdentityHashMap<>();
		for (var t : mainFlow) {
			outCountByStatus.merge(t.getSourceStatus(), 1, Integer::sum);
			inCountByStatus.merge(t.getTargetStatus(), 1, Integer::sum);
		}

		for (var s : wf.getStatuses()) {
			int inCount = inCountByStatus.getOrDefault(s, 0);
			int outCount = outCountByStatus.getOrDefault(s, 0);

			if (s == initialStatus) {
				if (!(inCount == 0 && outCount <= 1)) {
					throw new InvalidOperationException("Initial must have inCount=0 and outCount<=1 in main flow.");
				}
			} else if (s.isTerminal()) {
				if (!(inCount <= 1 && outCount == 0)) {
					throw new InvalidOperationException("Terminal must have inCount<=1 and outCount=0 in main flow.");
				}
			} else {
				if (!((inCount == 0 && outCount == 0) || (inCount == 1 && outCount == 1))) {
					throw new InvalidOperationException("Intermediate must be (1,1) or (0,0) on main flow.");
				}
			}
		}

		Map<WorkflowStatus, WorkflowTransition> nextTransition = new IdentityHashMap<>();
		for (var t : mainFlow) {
			if (nextTransition.put(t.getSourceStatus(), t) != null) {
				throw new InvalidOperationException("Multiple main transitions leaving the same status.");
			}
		}

		int visitedCount = 0;
		Set<WorkflowTransition> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		WorkflowStatus currentStatus = initialStatus;
		while (true) {
			WorkflowTransition transition = nextTransition.get(currentStatus);
			if (transition == null) {
				break;
			}
			if (!visited.add(transition)) {
				throw new InvalidOperationException("Cycle detected in main flow.");
			}
			visitedCount++;
			currentStatus = transition.getTargetStatus();
		}

		if (visitedCount != mainFlow.size()) {
			throw new InvalidOperationException(
				"Main flow must be a single straight line (disconnected transitions present).");
		}

		if (!currentStatus.isTerminal()) {
			throw new InvalidOperationException("Main flow must end at a terminal status.");
		}
	}
}
