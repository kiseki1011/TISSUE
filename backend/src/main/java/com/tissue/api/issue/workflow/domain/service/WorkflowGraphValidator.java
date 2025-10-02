package com.tissue.api.issue.workflow.domain.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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

	public record EntityRef(Long id, String tempKey) {
		public EntityRef {
			if ((id == null) == (tempKey == null)) {
				throw new IllegalArgumentException("One of id or tempKey must be provided");
			}
		}

		public boolean isExisting() {
			return id != null;
		}
	}

	public record StatusInfo(String ref, boolean initial, boolean terminal) {
	}

	public record TransitionInfo(String sourceRef, String targetRef) {
	}

	// 입력 기초 검증: "요청 바디가 형식적으로 맞는지"만 빠르게 확인
	public void validateWorkflowGraphStructure(List<StatusInfo> stsInfos, List<TransitionInfo> trsInfos) {
		ensureExactlyOneInitial(stsInfos);
		ensureAtLeastOneTerminal(stsInfos);
		ensureTransitionReferencesValid(stsInfos, trsInfos);
		ensureNoSelfLoops(trsInfos);
	}

	/**
	 * 워크플로우 그래프의 핵심 불변식을 한 번에 보장한다.
	 * - mainFlow 후보들이 '한 줄'인지
	 * - 그 끝이 terminal인지, 그리고 해당 main flow terminal에서 out=0인지
	 * - initial로 들어오는 전이가 없는지
	 * - 고아 상태가 없는지
	 */
	public WorkflowStatus ensureValidWorkflowGraph(Workflow wf, List<WorkflowTransition> mainCandidates) {
		ensureMainFlowSingleLine(wf, mainCandidates); // mainFlow 후보들이 '한 줄'인지
		WorkflowStatus mainTerminal = ensureMainTerminalExists(wf,
			mainCandidates); // 끝이 terminal인지, 해당 terminal에서 out=0인지
		ensureNoInToInitial(wf.getInitialStatus(), wf.getTransitions());
		ensureNoOrphans(wf);
		return mainTerminal;
	}

	/**
	 * 삭제 금지: "initial/main flow terminal"는 삭제 금지
	 *  - "요청에 포함되지 않아 삭제될 status들" 중에 initial이나 main flow terminal이 있으면 예외를 던진다
	 */
	public void ensureNotDeletingInitialOrMainTerminal(
		Set<WorkflowStatus> toDelete,
		WorkflowStatus initial,
		WorkflowStatus mainTerminal
	) {
		if (toDelete.contains(initial)) {
			throw new InvalidOperationException("Cannot delete the initial status.");
		}
		if (mainTerminal != null && toDelete.contains(mainTerminal)) {
			throw new InvalidOperationException("Cannot delete the main-flow terminal.");
		}
	}

	private void ensureExactlyOneInitial(List<StatusInfo> stsInfos) {
		long initialCount = stsInfos.stream().filter(StatusInfo::initial).count();
		if (initialCount != 1) {
			throw new InvalidOperationException("Exactly one initial required.");
		}
	}

	private void ensureAtLeastOneTerminal(List<StatusInfo> stsInfos) {
		long count = stsInfos.stream().filter(StatusInfo::terminal).count();
		if (count == 0) {
			throw new InvalidOperationException("At least one terminal required.");
		}
	}

	// transition이 가리키는 key들이 실제 status 키 집합 안에 존재하는지 확인
	private void ensureTransitionReferencesValid(List<StatusInfo> stsInfos, List<TransitionInfo> trsInfos) {
		Set<String> refs = stsInfos.stream()
			.map(StatusInfo::ref)
			.collect(Collectors.toSet());

		if (refs.size() != stsInfos.size()) {
			throw new InvalidOperationException("Duplicate status keys found.");
		}

		for (var t : trsInfos) {
			if (!refs.contains(t.sourceRef())) {
				throw new InvalidOperationException("Unknown source reference: " + t.sourceRef());
			}
			if (!refs.contains(t.targetRef())) {
				throw new InvalidOperationException("Unknown target reference: " + t.targetRef());
			}
		}
	}

	private void ensureNoSelfLoops(List<TransitionInfo> trsInfos) {
		for (var t : trsInfos) {
			if (Objects.equals(t.sourceRef(), t.targetRef())) {
				throw new InvalidOperationException("Self-loop not allowed.");
			}
		}
	}

	/**
	 * 메인 플로우 후보 검증: mainFlowCandidates들이 "initial → ... → terminal"로 이어지는 싱글 라인인지 검사
	 *  - 비어 있으면 안됨
	 *  - 분기(한 상태에서 두 갈래로 나감) 금지
	 *  - 사이클 금지
	 *  - 끊김 금지
	 */
	private void ensureMainFlowSingleLine(Workflow wf, List<WorkflowTransition> mainCandidates) {
		if (mainCandidates == null || mainCandidates.isEmpty()) {
			throw new InvalidOperationException("Main flow must not be empty.");
		}

		// 초기 상태가 설정되어 있는지(그리고 살아있는지)
		WorkflowStatus initial = requireInitialSet(wf);

		// "어느 상태에서 어떤 mainFlow 전이로 나가는지"를 빠르게 찾기 위해 source→transition 맵으로 만든다.
		Map<WorkflowStatus, WorkflowTransition> next = buildNextMap(mainCandidates);

		// 초기에서 출발해서 next를 따라 한 칸씩 걷는다.
		// - 본 적 있는 전이를 또 만나면 사이클
		// - 끝까지 걸었는데 후보 개수만큼 못 걸었다면 끊김/분기
		int walked = 0;
		Set<WorkflowTransition> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		WorkflowStatus cur = initial;
		while (true) {
			WorkflowTransition step = next.get(cur);
			if (step == null) { // 더 이상 이어지는 전이가 없음 → 도착
				break;
			}
			if (!seen.add(step)) { // 이미 본 전이를 또 봤다 → 사이클
				throw new InvalidOperationException("Cycle detected in main flow.");
			}
			walked++;
			cur = step.getTargetStatus();
		}
		if (walked != mainCandidates.size()) { // 후보 전이 수만큼 정확히 걸어야 '한 줄'이다.
			throw new InvalidOperationException("Main flow must be one straight path (disconnected segments present).");
		}
	}

	/**
	 * main flow의 마지막 노드를 계산해서 반환한다.
	 * - 전제: ensureMainFlowSingleLine(...)을 먼저 호출해 '한 줄'임이 보장되어 있어야 한다.
	 * - 보장: 계산된 마지막 노드는 반드시 terminal이어야 하며, 아니라면 예외를 던진다.
	 */
	private WorkflowStatus ensureMainTerminalExists(Workflow wf, List<WorkflowTransition> mainCandidates) {
		WorkflowStatus initial = requireInitialSet(wf); // 초기 상태 존재/활성 보장

		// source -> next transition 매핑 (ensureMainFlowSingleLine에서 분기/사이클/불연속 이미 걸렀지만, 여기선 순수 계산만)
		Map<WorkflowStatus, WorkflowTransition> next = new IdentityHashMap<>();
		for (var t : mainCandidates) {
			next.put(t.getSourceStatus(), t);
		}

		// initial에서 한 칸씩 전진하여 마지막 상태를 찾는다.
		WorkflowStatus cur = initial;
		for (int guard = 0; guard <= mainCandidates.size(); guard++) {
			WorkflowTransition step = next.get(cur);
			if (step == null) {
				break; // 더 나갈 전이가 없으면 cur가 마지막
			}
			cur = step.getTargetStatus();
		}

		// 마지막은 반드시 terminal이어야 한다(아니면 도메인 규칙 위반)
		if (cur == null || !cur.isTerminal()) {
			throw new InvalidOperationException("Main flow must end at a terminal status.");
		}

		ensureNoOutFromMainTerminal(cur, wf.getTransitions());

		return cur;
	}

	// Initial로 들어오는 전이 금지 (초기 상태의 in-degree는 0)
	private void ensureNoInToInitial(WorkflowStatus initial, Collection<WorkflowTransition> all) {
		for (var t : all) {
			if (t.getTargetStatus().equals(initial)) {
				throw new InvalidOperationException("Transitions into the initial status are not allowed.");
			}
		}
	}

	// 메인플로우 '터미널'에서 나가는 전이 금지 (터미널의 out-degree는 0)
	private void ensureNoOutFromMainTerminal(WorkflowStatus mainTerminal, Collection<WorkflowTransition> all) {
		for (var t : all) {
			if (t.getSourceStatus().equals(mainTerminal)) {
				throw new InvalidOperationException("Transitions out of the main-flow terminal are not allowed.");
			}
		}
	}

	// TODO: 이런 고아 상태 체킹을 위한 미리 만들어진 검증 메서드는 없나?
	// 고아 상태 금지
	// - 최종 그래프 기준으로 initial에서 출발해 모든 살아있는 상태가 도달 가능해야 한다.
	// - 이유: 보드/리포트/이동 로직에서 "연결 안 된 섬"이 있으면 사용자가 낭패를 봄
	private void ensureNoOrphans(Workflow wf) {
		WorkflowStatus initial = requireInitialSet(wf);

		// 인접 리스트(그래프)를 만든다. (archived=true는 제외)
		Map<Long, List<Long>> adj = new HashMap<>();
		for (var t : wf.getTransitions()) {
			if (t.isArchived()) {
				continue;
			}
			adj.computeIfAbsent(t.getSourceStatus().getId(), k -> new ArrayList<>())
				.add(t.getTargetStatus().getId());
		}

		// BFS로 initial에서 시작해 도달 가능한 상태를 모두 방문
		Set<Long> visited = new HashSet<>();
		Deque<Long> deque = new ArrayDeque<>();
		deque.add(initial.getId());
		visited.add(initial.getId());
		while (!deque.isEmpty()) {
			Long u = deque.poll();
			for (Long v : adj.getOrDefault(u, List.of())) {
				if (visited.add(v)) {
					deque.add(v);
				}
			}
		}

		// 살아있는 상태 수와 방문한 상태 수가 같아야 '고아 없음'
		long alive = wf.getStatuses().stream().filter(s -> !s.isArchived()).count();
		if (visited.size() != alive) {
			throw new InvalidOperationException("Orphan statuses exist (unreachable from initial).");
		}
	}

	// 초기 상태는 반드시 있어야 하고(null X), 살아있어야(archived=false) 한다.
	private WorkflowStatus requireInitialSet(Workflow wf) {
		WorkflowStatus status = wf.getInitialStatus();
		if (status == null || status.isArchived()) {
			throw new InvalidOperationException("Initial must exist and be active.");
		}
		return status;
	}

	// mainFlow 전이를 "source → 다음 전이"로 빠르게 찾기 위한 맵 구성
	private Map<WorkflowStatus, WorkflowTransition> buildNextMap(List<WorkflowTransition> main) {
		Map<WorkflowStatus, WorkflowTransition> next = new IdentityHashMap<>();
		// TODO: 분기 금지를 위한 검증은 따로 메서드로 추출해서 사용해야 의미가 명확하지 않을까?
		for (var t : main) {
			// 같은 source에서 두 개 이상 나가면 '분기' → 금지
			if (next.put(t.getSourceStatus(), t) != null) {
				throw new InvalidOperationException("Main flow must be a single line (branching forbidden).");
			}
		}
		return next;
	}
}
