package com.tissue.api.issue.workflow.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.ReplaceWorkflowGraphCommand;
import com.tissue.api.issue.workflow.application.finder.WorkflowFinder;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;
import com.tissue.api.issue.workflow.domain.model.WorkflowTransition;
import com.tissue.api.issue.workflow.domain.service.WorkflowGraphValidator;
import com.tissue.api.issue.workflow.presentation.dto.response.WorkflowResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowGraphEditService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowGraphValidator graphValidator;

	// 목표:
	// 1) 요청대로 그래프를 "다시 조립"
	// 2) 우리가 세운 규칙을 "한 번" 강하게 검증
	// 3) 통과하면 메인 플로우를 "원자적으로" 적용하고 커밋
	@Transactional
	public WorkflowResponse replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd) {

		// 워크플로우 로드 + @Version 확인(동시 수정 충돌 방지)
		Workflow wf = loadWorkflowAndCheckVersion(cmd);

		// 요청 바디의 최소 형식 검증(빠른 실패) — 무거운 작업 전에 가벼운 체크
		graphValidator.validateWorkflowGraphStructure(
			cmd.replaceStatusCommands().stream().map(s -> s.toInfo()).toList(),
			cmd.replaceTransitionCommands().stream().map(t -> t.toInfo()).toList()
		);

		// 상태부터 물리화(materialize)
		WorkflowGraphEditService.StatusResolver statusResolver = materializeStatuses(wf, cmd.replaceStatusCommands());

		// 전이 업서트(있으면 재배선 / 없으면 신규) + 요청에 없는 기존 전이 삭제(소프트)
		// - 기존 전이는 label/description 변경하지 않음(정책)
		// - 신규 전이만 label/description 사용
		// - mainFlow=true 표시된 전이들은 따로 모아둔다(후보 목록)
		List<WorkflowTransition> mainCandidates = upsertTransitionsAndCollectMainCandidates(
			wf,
			cmd.replaceTransitionCommands(),
			statusResolver
		);

		// 기존 상태의 terminal 플래그만 반영 (initial은 아래에서 한 번에 반영)
		applyTerminalFlagChanges(wf, cmd.replaceStatusCommands(), statusResolver);

		// 요청된 initial을 해석하고, 실제로 initial로 반영(카디널리티 1 보장)
		WorkflowStatus requestedInitial = resolveAndApplyInitial(wf, cmd.replaceStatusCommands(), statusResolver);

		// 메인 플로우 검증 → 한 줄인지 먼저 체크, 그 후 정책 검증(최종 그래프 기준)
		WorkflowStatus mainTerminal = graphValidator.ensureValidWorkflowGraph(wf, mainCandidates);

		// 삭제 대상 계산(요청에 없는 기존 status) + "initial/메인터미널 삭제 금지" 규칙 적용
		Set<WorkflowStatus> toDelete = findStatusesToDelete(wf, cmd.replaceStatusCommands());
		graphValidator.ensureNotDeletingInitialOrMainTerminal(toDelete, requestedInitial, mainTerminal);
		toDelete.forEach(WorkflowStatus::softDelete);

		// 메인 플로우 실제 적용(원자적으로: 전부 off → 후보만 on)
		wf.defineMainFlow(mainCandidates);

		return WorkflowResponse.from(wf);
	}

	// 워크플로우 로드 + @Version 체크
	// - 버전이 다르면 409(충돌) → 누군가 먼저 저장했으니 사용자는 새로고침 해야 함
	private Workflow loadWorkflowAndCheckVersion(ReplaceWorkflowGraphCommand cmd) {
		Workspace ws = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow wf = workflowFinder.findWorkflow(ws, cmd.workflowId());

		if (!Objects.equals(wf.getVersion(), cmd.version())) {
			throw new IllegalStateException("Version mismatch");
		}
		return wf;
	}

	// 상태 materialize
	// - 기존(id) 상태는 레퍼런스에 넣기만 하고
	// - 신규(tempKey=UUID) 상태는 실제 생성(이때만 label/description을 사용)
	private WorkflowGraphEditService.StatusResolver materializeStatuses(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.ReplaceStatusCommand> statusCmds
	) {
		Map<Long, WorkflowStatus> statusById = new HashMap<>();
		Map<String, WorkflowStatus> statusByTempKey = new HashMap<>();

		for (WorkflowStatus s : wf.getStatuses()) {
			statusById.put(s.getId(), s);
		}

		for (var s : statusCmds) {
			if (s.ref().isExisting()) {
				continue;  // 기존은 label/description 변경 금지
			}
			WorkflowStatus created = wf.addStatus(
				Label.of(s.label()),
				s.description(),
				s.initial(),
				s.terminal()
			);
			statusByTempKey.put(s.ref().tempKey(), created);
		}

		return new WorkflowGraphEditService.StatusResolver(statusById, statusByTempKey);
	}

	// 전이 upsert + mainFlow 후보 수집
	// - 요청에 없는 기존 전이는 soft-delete
	// - 기존 전이는 '재배선'만
	// - 신규 전이는 생성(라벨/설명 사용 OK)
	// - mainFlow=true는 후보 리스트에 모아두고, 마지막에 원자적으로 적용
	private List<WorkflowTransition> upsertTransitionsAndCollectMainCandidates(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.ReplaceTransitionCommand> transitionCmds,
		WorkflowGraphEditService.StatusResolver statusResolver
	) {
		// 요청에 없는 기존 전이 삭제
		Set<Long> reqIds = transitionCmds.stream()
			.map(t -> t.ref().id())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		for (WorkflowTransition t : List.copyOf(wf.getTransitions())) {
			if (t.getId() != null && !reqIds.contains(t.getId())) {
				t.softDelete();
			}
		}

		// 기존 전이 인덱스
		Map<Long, WorkflowTransition> transById = new HashMap<>();
		for (WorkflowTransition t : wf.getTransitions()) {
			if (t.getId() != null) {
				transById.put(t.getId(), t);
			}
		}

		// upsert + 후보 수집
		List<WorkflowTransition> mainCandidates = new ArrayList<>();
		for (var t : transitionCmds) {
			WorkflowStatus src = statusResolver.resolve(t.source());
			WorkflowStatus trg = statusResolver.resolve(t.target());

			WorkflowTransition transition;
			if (t.ref().isExisting()) {
				// 기존 전이는 '재배선'만
				transition = transById.get(t.ref().id());
				if (transition == null) {
					throw new InvalidOperationException("Unknown transition id: " + t.ref().id());
				}
				wf.rewireTransitionSource(transition, src);
				wf.rewireTransitionTarget(transition, trg);
			} else {
				// 신규 전이는 생성(이때만 label/description 사용)
				transition = wf.addTransition(Label.of(t.label()), t.description(), src, trg);
			}

			if (t.mainFlow()) {
				mainCandidates.add(transition);
			}
		}
		return mainCandidates;
	}

	// 기존 상태의 terminal 플래그만 반영한다.
	// - 이유: initial은 resolveAndApplyInitial에서 '한 번에' 반영해야 1개 보장이 쉬움
	private void applyTerminalFlagChanges(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.ReplaceStatusCommand> statusCmds,
		WorkflowGraphEditService.StatusResolver statusResolver
	) {
		for (var cmd : statusCmds) {
			if (!cmd.ref().isExisting()) {
				continue;
			}
			WorkflowStatus status = statusResolver.statusById().get(cmd.ref().id());
			if (status == null) {
				throw new InvalidOperationException("Unknown status id: " + cmd.ref().id());
			}
			if (status.isTerminal() != cmd.terminal()) {
				if (cmd.terminal()) {
					wf.markStatusTerminal(status);
				} else {
					wf.unmarkStatusTerminal(status);
				}
			}
		}
	}

	// 요청된 initial을 실제 initial로 반영(카디널리티 1 보장: 기존 전부 false → 새 것만 true)
	private WorkflowStatus resolveAndApplyInitial(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.ReplaceStatusCommand> statusCmds,
		WorkflowGraphEditService.StatusResolver statusResolver
	) {
		var statusCmd = statusCmds.stream()
			.filter(ReplaceWorkflowGraphCommand.ReplaceStatusCommand::initial)
			.findFirst()
			.orElseThrow(() -> new InvalidOperationException("Initial not provided"));

		WorkflowStatus requested = statusResolver.resolve(statusCmd.ref());

		if (requested != wf.getInitialStatus()) {
			wf.updateInitialStatus(requested);
		}
		return requested;
	}

	// 삭제 대상 계산(요청 목록에 없는 기존 상태들)
	private Set<WorkflowStatus> findStatusesToDelete(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.ReplaceStatusCommand> statusCmds
	) {
		Set<Long> keepIds = statusCmds.stream()
			.map(cmd -> cmd.ref().id())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		return wf.getStatuses().stream()
			.filter(s -> !s.isArchived())
			.filter(s -> s.getId() != null && !keepIds.contains(s.getId()))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	// TODO: 웬만하면 가독성을 위해 early return 활용하자
	//  (조건문 내 조건문 같은 스타일은 피하자)
	private record StatusResolver(
		Map<Long, WorkflowStatus> statusById,
		Map<String, WorkflowStatus> statusByTempKey
	) {
		WorkflowStatus resolve(WorkflowGraphValidator.EntityRef ref) {
			WorkflowStatus status;
			if (ref.isExisting()) {
				status = statusById.get(ref.id());
				if (status == null) {
					throw new InvalidOperationException("Unknown status id: " + ref.id());
				}
			} else {
				status = statusByTempKey.get(ref.tempKey());
				if (status == null) {
					throw new InvalidOperationException(
						"Unknown status tempKey: " + ref.tempKey());
				}
			}
			return status;
		}
	}
}
