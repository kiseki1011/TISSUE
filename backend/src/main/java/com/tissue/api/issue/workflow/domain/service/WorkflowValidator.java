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

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.application.dto.ReplaceWorkflowGraphCommand;
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

	// [입력 기초 검증]
	// 이 단계의 목표: "요청 바디가 형식적으로 맞는지"만 빠르게 확인.
	// - initial은 딱 1개여야 하고
	// - terminal은 최소 1개 있어야 하며
	// - transition이 가리키는 source/target은 실제로 요청된 status 중 하나여야 함
	// - self-loop(자기 자신에게 향하는 전이)는 금지
	public void validateReplaceBasics(ReplaceWorkflowGraphCommand cmd) {
		ensureExactlyOneInitial(cmd);          // initial == 1
		ensureAtLeastOneTerminal(cmd);        // terminal >= 1
		ensureTransitionReferencesValid(cmd);  // 전이가 가리키는 키가 유효한지
		ensureNoSelfLoops(cmd);                // self-loop 금지
	}

	/**
	 * 워크플로우 그래프의 핵심 불변식을 한 번에 보장한다.
	 * - mainFlow 후보가 '한 줄'인지
	 * - 그 끝이 terminal인지, 그리고 메인 터미널에서 out=0인지
	 * - initial로 들어오는 전이가 없는지
	 * - 고아 상태가 없는지
	 *
	 * @return 계산된 '메인 플로우 터미널' (필요 없으면 호출부에서 무시)
	 * @throws InvalidOperationException 위의 불변식 중 하나라도 위반 시
	 */
	public WorkflowStatus ensureValidWorkflowGraph(Workflow wf, List<WorkflowTransition> mainCandidates) {
		ensureMainFlowSingleLine(wf, mainCandidates);
		WorkflowStatus mainTerminal = requireMainFlowTerminal(wf, mainCandidates); // 내부에서 terminal 종료 + 터미널 out=0 보장
		ensureNoIncomingToInitial(wf.getInitialStatus(), wf.getTransitions());
		ensureNoOrphans(wf);
		return mainTerminal;
	}

	// [메인 플로우: 검증]
	// "mainFlow 후보 전이들"이 '초기 → ... → 터미널'로 이어지는 한 줄짜리 길인지 검사.
	// - 비어있으면 안 되고
	// - 분기(한 상태에서 두 갈래로 나감) 금지
	// - 사이클(빙빙 돎) 금지
	// - 끊김(중간에 뚝 끊긴 구간) 금지
	private void ensureMainFlowSingleLine(Workflow wf, List<WorkflowTransition> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			throw new InvalidOperationException("Main flow must not be empty.");
		}

		// 초기 상태가 설정되어 있는지(그리고 살아있는지)
		WorkflowStatus initial = requireInitialSet(wf);

		// "어느 상태에서 어떤 mainFlow 전이로 나가는지"를 빠르게 찾기 위해 source→transition 맵으로 만든다.
		Map<WorkflowStatus, WorkflowTransition> next = buildNextMap(candidates);

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
		if (walked != candidates.size()) { // 후보 전이 수만큼 정확히 걸어야 '한 줄'이다.
			throw new InvalidOperationException("Main flow must be one straight path (disconnected segments present).");
		}
	}

	/**
	 * main flow의 마지막 노드를 계산해서 반환한다.
	 * - 전제: ensureMainFlowSingleLine(...)을 먼저 호출해 '한 줄'임이 보장되어 있어야 한다.
	 * - 보장: 계산된 마지막 노드는 반드시 terminal이어야 하며, 아니라면 예외를 던진다.
	 */
	private WorkflowStatus requireMainFlowTerminal(Workflow wf, List<WorkflowTransition> candidates) {
		WorkflowStatus initial = requireInitialSet(wf); // 초기 상태 존재/활성 보장

		// source -> next transition 매핑 (ensureMainFlowSingleLine에서 분기/사이클/불연속 이미 걸렀지만, 여기선 순수 계산만)
		Map<WorkflowStatus, WorkflowTransition> next = new IdentityHashMap<>();
		for (var t : candidates) {
			next.put(t.getSourceStatus(), t);
		}

		// initial에서 한 칸씩 전진하여 마지막 상태를 찾는다.
		WorkflowStatus cur = initial;
		for (int guard = 0; guard <= candidates.size(); guard++) {
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

		ensureNoOutgoingFromMainTerminal(cur, wf.getTransitions());

		return cur;
	}

	// [정책 검증: Initial/메인터미널 제약 + 고아 금지]

	// 규칙1: Initial로 들어오는 전이 금지 (초기 상태의 in-degree는 0)
	private void ensureNoIncomingToInitial(WorkflowStatus initial, Collection<WorkflowTransition> all) {
		for (var t : all) {
			if (t.getTargetStatus().equals(initial)) {
				throw new InvalidOperationException("Transitions into the initial status are not allowed.");
			}
		}
	}

	// 규칙2: 메인플로우 '터미널'에서 나가는 전이 금지 (터미널의 out-degree는 0)
	private void ensureNoOutgoingFromMainTerminal(WorkflowStatus mainTerminal,
		Collection<WorkflowTransition> allTransitions) {
		for (var t : allTransitions) {
			if (t.getSourceStatus().equals(mainTerminal)) {
				throw new InvalidOperationException("Transitions out of the main-flow terminal are not allowed.");
			}
		}
	}

	// 규칙3: 고아 상태 금지
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

	// [삭제 금지: initial / 메인터미널]
	// "요청에 포함되지 않아 삭제될 상태들" 중에 initial이나 mainTerminal가 있으면 막는다.
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

	// [private helpers]
	private void ensureExactlyOneInitial(ReplaceWorkflowGraphCommand cmd) {
		long initialCount = cmd.statuses().stream().filter(ReplaceWorkflowGraphCommand.StatusCmd::initial).count();
		if (initialCount != 1) {
			throw new InvalidOperationException("Exactly one initial required.");
		}
	}

	private void ensureAtLeastOneTerminal(ReplaceWorkflowGraphCommand cmd) {
		long terminalCount = cmd.statuses().stream().filter(ReplaceWorkflowGraphCommand.StatusCmd::terminal).count();
		if (terminalCount == 0) {
			throw new InvalidOperationException("At least one terminal required.");
		}
	}

	// transition이 가리키는 key들이 실제 status 키 집합 안에 존재하는지 확인
	private void ensureTransitionReferencesValid(ReplaceWorkflowGraphCommand cmd) {
		Set<String> keys = new HashSet<>();
		for (var s : cmd.statuses()) {
			String key = s.id() != null ? "id:" + s.id() : "tmp:" + s.tempKey(); // id는 DB기존, tmp는 신규(클라 UUID)
			if (!keys.add(key)) {
				throw new InvalidOperationException("Duplicate status key: " + key);
			}
		}
		for (var t : cmd.transitions()) {
			String srcKey = normalizeKey(t.sourceKey()); // "id:###" 또는 "tmp:###"로 표준화
			String trgKey = normalizeKey(t.targetKey());
			if (!keys.contains(srcKey)) {
				throw new InvalidOperationException("Unknown source: " + t.sourceKey());
			}
			if (!keys.contains(trgKey)) {
				throw new InvalidOperationException("Unknown target: " + t.targetKey());
			}
		}
	}

	private void ensureNoSelfLoops(ReplaceWorkflowGraphCommand cmd) {
		for (var t : cmd.transitions()) {
			String srcKey = normalizeKey(t.sourceKey());
			String trgKey = normalizeKey(t.targetKey());
			if (Objects.equals(srcKey, trgKey)) {
				throw new InvalidOperationException("Self-loop not allowed.");
			}
		}
	}

	// 초기 상태는 반드시 있어야 하고(널X), 살아있어야(archived=false) 한다.
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
		for (var t : main) {
			// 같은 source에서 두 개 이상 나가면 '분기' → 금지
			if (next.put(t.getSourceStatus(), t) != null) {
				throw new InvalidOperationException("Main flow must be a single line (branching forbidden).");
			}
		}
		return next;
	}

	// "id:###", "tmp:###"처럼 표준화해서 맵 키로 쓰기 쉽게 만든다.
	// - 이유: 클라가 "123", "tmp-uuid" 같은 값만 보내와도 안정적으로 처리하려고.
	private String normalizeKey(String key) {
		if (key == null) {
			throw new InvalidOperationException("key is null");
		}
		return (key.startsWith("id:") || key.startsWith("tmp:")) ? key : ("tmp:" + key);
	}
}
