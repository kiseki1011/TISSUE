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

	public void validateCommand(CreateWorkflowCommand cmd) {
		// 라벨 중복/카디널리티
		Set<Label> labels = new HashSet<>();
		int initial = 0;
		int terminal = 0;
		for (var s : cmd.statuses()) {
			if (!labels.add(s.label())) {
				throw new DuplicateResourceException("Duplicate step label: " + s.label());
			}
			if (s.initial()) {
				initial++;
			}
			if (s.terminal()) {
				terminal++;
			}
		}
		if (initial != 1) {
			throw new InvalidOperationException("Workflow must have a single initial step.");
		}
		if (terminal == 0) {
			throw new InvalidOperationException("Workflow must have at least one terminal step.");
		}

		// tempKey 유효/유니크
		Set<String> keys = new HashSet<>();
		for (var s : cmd.statuses()) {
			if (s.tempKey() == null || s.tempKey().isBlank()) {
				throw new InvalidOperationException("Status tempKey must be non-blank.");
			}
			if (!keys.add(s.tempKey())) {
				throw new DuplicateResourceException("Duplicate status tempKey: " + s.tempKey());
			}
		}

		// 전이 참조 무결성
		for (var t : cmd.transitions()) {
			if (t.sourceTempKey() == null || t.sourceTempKey().isBlank()) {
				throw new InvalidOperationException("Transition sourceTempKey must be non-blank.");
			}
			if (t.targetTempKey() == null || t.targetTempKey().isBlank()) {
				throw new InvalidOperationException("Transition targetTempKey must be non-blank.");
			}

			if (!keys.contains(t.sourceTempKey())) {
				throw new InvalidOperationException("Unknown source tempKey: " + t.sourceTempKey());
			}
			if (!keys.contains(t.targetTempKey())) {
				throw new InvalidOperationException("Unknown target tempKey: " + t.targetTempKey());
			}
			// (선택) self-loop 금지:
			// if (t.sourceTempKey().equals(t.targetTempKey())) throw new InvalidOperationException("Self-loop not allowed.");
		}
	}

	/**
	 * 메인 플로우가 '초기 → ... → (종료)'로 이어지는 단일 직선 경로임을 보장한다.
	 * 조건 불만족 시 예외를 던진다.
	 * 경로가 비어 있으면 안됨(최소한 두 개의 status, 그러니깐 하나의 transition으로 이루어져야 함)
	 */
	public void ensureMainFlowSingleLine(Workflow wf, List<WorkflowTransition> main) {
		if (main.isEmpty()) {
			return;
		}

		WorkflowStatus initialStatus = wf.getInitialStatus();
		if (initialStatus == null) {
			throw new InvalidOperationException("Initial status must be set before validating main flow.");
		}

		// 소속/상태 집합 확인
		Set<WorkflowStatus> statusSet = new HashSet<>(wf.getStatuses());
		for (var t : main) {
			if (t.getWorkflow() != wf) {
				throw new InvalidOperationException("Foreign transition in main flow.");
			}
			if (!statusSet.contains(t.getSourceStatus()) || !statusSet.contains(t.getTargetStatus())) {
				throw new InvalidOperationException("Main flow must connect statuses of this workflow.");
			}
		}

		// 차수 계산
		Map<WorkflowStatus, Integer> inDeg = new IdentityHashMap<>();
		Map<WorkflowStatus, Integer> outDeg = new IdentityHashMap<>();
		for (var t : main) {
			outDeg.put(t.getSourceStatus(), outDeg.getOrDefault(t.getSourceStatus(), 0) + 1);
			inDeg.put(t.getTargetStatus(), inDeg.getOrDefault(t.getTargetStatus(), 0) + 1);
		}

		// 차수 제약
		for (var s : wf.getStatuses()) {
			int in = inDeg.getOrDefault(s, 0);
			int out = outDeg.getOrDefault(s, 0);

			if (s == initialStatus) {
				if (!(in == 0 && out <= 1)) {
					throw new InvalidOperationException("Initial must have in=0 and out<=1 in main flow.");
				}
			} else if (s.isTerminal()) {
				if (!((in <= 1 && out == 0) || (in == 0 && out == 0))) {
					throw new InvalidOperationException("Terminal must have in<=1 and out=0 in main flow.");
				}
			} else {
				if (!((in == 0 && out == 0) || (in == 1 && out == 1))) {
					throw new InvalidOperationException("Intermediate must be (1,1) or (0,0) on main flow.");
				}
			}
		}

		// 연결성/무순환/도달성
		Map<WorkflowStatus, WorkflowTransition> nextEdge = new IdentityHashMap<>();
		for (var t : main) {
			if (nextEdge.put(t.getSourceStatus(), t) != null) {
				throw new InvalidOperationException("Multiple main edges leaving the same status.");
			}
		}

		int walked = 0;
		Set<WorkflowTransition> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		WorkflowStatus cur = initialStatus;
		while (true) {
			WorkflowTransition e = nextEdge.get(cur);
			if (e == null) {
				break;
			}
			if (!visited.add(e)) {
				throw new InvalidOperationException("Cycle detected in main flow.");
			}
			walked++;
			cur = e.getTargetStatus();
		}

		if (walked != main.size()) {
			throw new InvalidOperationException(
				"Main flow must be a single straight line (disconnected edges present).");
		}

		if (!cur.isTerminal()) {
			throw new InvalidOperationException("Main flow must end at a terminal status.");
		}
	}
}
