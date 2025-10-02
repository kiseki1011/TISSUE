package com.tissue.api.issue.workflow.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.application.dto.ReplaceWorkflowGraphCommand;
import com.tissue.api.issue.workflow.application.finder.WorkflowFinder;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;
import com.tissue.api.issue.workflow.domain.model.WorkflowTransition;
import com.tissue.api.issue.workflow.domain.service.WorkflowValidator;
import com.tissue.api.issue.workflow.infrastructure.repository.WorkflowRepository;
import com.tissue.api.issue.workflow.presentation.dto.response.WorkflowResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowRepository workflowRepository;
	private final WorkflowValidator validator;

	@Transactional
	public WorkflowResponse create(CreateWorkflowCommand cmd) {
		validator.validateCommand(cmd);
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		// validator.ensureLabelUnique(workspace, cmd.label());

		try {
			Workflow workflow = workflowRepository.save(
				Workflow.create(workspace, cmd.label(), cmd.description())
			);

			Map<String, WorkflowStatus> statusMap = new HashMap<>();
			for (CreateWorkflowCommand.StatusCommand s : cmd.statuses()) {
				WorkflowStatus status = workflow.addStatus(s.label(), s.description(), s.initial(), s.terminal());
				statusMap.put(s.tempKey(), status);
			}

			List<WorkflowTransition> mainFlowCandidates = new ArrayList<>();

			for (CreateWorkflowCommand.TransitionCommand t : cmd.transitions()) {
				WorkflowStatus src = statusMap.get(t.sourceTempKey());
				WorkflowStatus trg = statusMap.get(t.targetTempKey());

				WorkflowTransition transition = workflow.addTransition(t.label(), t.description(), src, trg);

				if (t.mainFlow()) {
					mainFlowCandidates.add(transition);
				}
			}

			validator.ensureValidWorkflowGraph(workflow, mainFlowCandidates);

			workflow.defineMainFlow(mainFlowCandidates);

			return WorkflowResponse.from(workflow);
		} catch (DataIntegrityViolationException e) {
			log.info("Failed due to duplicate label.", e);
			throw new DuplicateResourceException("Duplicate label is not allowed.", e);
		}
	}

	// 목표:
	// 1) 요청대로 그래프를 "다시 조립"
	// 2) 우리가 세운 규칙을 "한 번" 강하게 검증
	// 3) 통과하면 메인 플로우를 "원자적으로" 적용하고 커밋
	@Transactional
	public WorkflowResponse replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd) {

		// [0] 워크플로우 로드 + @Version 확인(동시 수정 충돌 방지)
		Workflow wf = loadWorkflowAndCheckVersion(cmd);

		// [1] 요청 바디의 최소 형식 검증(빠른 실패) — 무거운 작업 전에 가벼운 체크
		validator.validateReplaceBasics(cmd);

		// [2] 상태부터 물리화(materialize)
		// - 기존 상태는 "id:##"로 ref에 넣고
		// - 신규 상태는 생성 후 "tmp:UUID"로 ref에 넣음
		Map<String, WorkflowStatus> statusRef = materializeStatuses(wf, cmd.statuses());

		// [3] 전이 업서트(있으면 재배선 / 없으면 신규) + 요청에 없는 기존 전이 삭제(소프트)
		// - 기존 전이는 label/description 변경하지 않음(정책)
		// - 신규 전이만 label/description 사용
		// - mainFlow=true 표시된 전이들은 따로 모아둔다(후보 목록)
		List<WorkflowTransition> mainCandidates = upsertTransitionsAndCollectMainCandidates(wf, cmd.transitions(),
			statusRef);

		// [4] 기존 상태의 terminal 플래그만 반영 (initial은 아래에서 한 번에 반영)
		applyTerminalFlagChanges(wf, cmd.statuses(), statusRef);

		// [5] 요청된 initial을 해석하고, 실제로 initial로 반영(카디널리티 1 보장)
		WorkflowStatus requestedInitial = resolveAndApplyInitial(wf, cmd.statuses(), statusRef);

		// [6] 메인 플로우 검증 → 한 줄인지 먼저 체크, 그 후 터미널 계산
		// [8] 정책 검증(최종 그래프 기준)
		WorkflowStatus mainTerminal = validator.ensureValidWorkflowGraph(wf, mainCandidates);

		// [7] 삭제 대상 계산(요청에 없는 기존 status) + "initial/메인터미널 삭제 금지" 규칙 적용
		Set<WorkflowStatus> toDelete = findStatusesToDelete(wf, cmd.statuses());
		validator.ensureNotDeletingInitialOrMainTerminal(toDelete, requestedInitial, mainTerminal);
		toDelete.forEach(WorkflowStatus::softDelete);

		// [9] 메인 플로우 실제 적용(원자적으로: 전부 off → 후보만 on)
		wf.defineMainFlow(mainCandidates);

		// [10] 스냅샷 응답
		return WorkflowResponse.from(wf);
	}

	// [0] 워크플로우 로드 + @Version 체크
	// - 버전이 다르면 409(충돌) → 누군가 먼저 저장했으니 사용자는 새로고침 해야 함
	private Workflow loadWorkflowAndCheckVersion(ReplaceWorkflowGraphCommand cmd) {
		Workspace ws = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow wf = workflowFinder.findWorkflow(ws, cmd.workflowId());

		if (!Objects.equals(wf.getVersion(), cmd.version())) {
			throw new IllegalStateException("Version mismatch");
		}
		return wf;
	}

	// [2] 상태 물리화
	// - 기존(id) 상태는 레퍼런스에 넣기만 하고
	// - 신규(tempKey=UUID) 상태는 실제 생성(이때만 label/description을 사용)
	private Map<String, WorkflowStatus> materializeStatuses(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StatusCmd> statuses
	) {
		Map<String, WorkflowStatus> ref = new HashMap<>();

		// 기존 상태: "id:##" 키로 맵핑
		for (WorkflowStatus s : wf.getStatuses()) {
			ref.put("id:" + s.getId(), s);
		}

		// 신규 상태: 실제 생성 후 "tmp:UUID" 키로 맵핑
		for (var s : statuses) {
			if (s.id() != null) {
				continue;
			} // 기존 것은 label/description 변경 금지(정책)
			WorkflowStatus created = wf.addStatus(
				Label.of(s.label()),   // 신규에만 라벨 사용
				s.description(),       // 신규에만 설명 사용
				s.initial(),
				s.terminal()
			);
			ref.put("tmp:" + s.tempKey(), created);
		}
		return ref;
	}

	// [3] 전이 업서트 + mainFlow 후보 수집
	// - 요청에 없는 기존 전이는 soft-delete
	// - 기존 전이는 '재배선'만 허용(라벨/설명 변경 금지)
	// - 신규 전이는 생성(라벨/설명 사용 OK)
	// - mainFlow=true는 후보 리스트에 모아두고, 마지막에 원자적으로 적용
	private List<WorkflowTransition> upsertTransitionsAndCollectMainCandidates(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.TransitionCmd> transitions,
		Map<String, WorkflowStatus> statusRef
	) {
		// 1) 요청에 없는 기존 전이 삭제
		Set<Long> reqIds = transitions.stream()
			.map(ReplaceWorkflowGraphCommand.TransitionCmd::id)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		for (WorkflowTransition t : List.copyOf(wf.getTransitions())) {
			if (t.getId() != null && !reqIds.contains(t.getId())) {
				t.softDelete();
			}
		}

		// 2) 기존 전이 인덱스(id→엔티티)
		Map<String, WorkflowTransition> transRef = new HashMap<>();
		for (WorkflowTransition t : wf.getTransitions()) {
			if (t.getId() != null) {
				transRef.put("id:" + t.getId(), t);
			}
		}

		// 3) 업서트 + 후보 수집
		List<WorkflowTransition> mainCandidates = new ArrayList<>();
		for (var t : transitions) {
			WorkflowStatus src = resolveStatus(statusRef, t.sourceKey()); // "id:##"/"tmp:UUID" 키로 실제 상태 얻기
			WorkflowStatus trg = resolveStatus(statusRef, t.targetKey());

			WorkflowTransition transition;
			if (t.id() != null) {
				// 기존 전이는 '재배선'만 허용
				transition = transRef.get("id:" + t.id());
				if (transition == null) {
					throw new InvalidOperationException("Unknown transition id: " + t.id());
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

	// [4] 기존 상태의 terminal 플래그만 반영한다.
	// - 이유: initial은 [5]에서 '한 번에' 반영해야 1개 보장이 쉬움
	private void applyTerminalFlagChanges(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StatusCmd> statusCmds,
		Map<String, WorkflowStatus> statusRef
	) {
		for (var cmd : statusCmds) {
			if (cmd.id() == null) {
				continue; // 신규는 생성 시 이미 반영됨
			}
			WorkflowStatus status = statusRef.get("id:" + cmd.id());
			if (status == null) {
				throw new InvalidOperationException("Unknown status id: " + cmd.id());
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

	// [5] 요청된 initial을 실제 initial로 반영(카디널리티 1 보장: 기존 전부 false → 새 것만 true)
	private WorkflowStatus resolveAndApplyInitial(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StatusCmd> statuses,
		Map<String, WorkflowStatus> statusRef
	) {
		var statusCmd = statuses.stream()
			.filter(ReplaceWorkflowGraphCommand.StatusCmd::initial)
			.findFirst()
			.orElseThrow(() -> new InvalidOperationException("Initial not provided"));

		// s.id가 있으면 "id:##", 없으면 "tmp:UUID"로 해석
		WorkflowStatus requested = resolveStatus(statusRef,
			statusCmd.id() != null ? "id:" + statusCmd.id() : "tmp:" + statusCmd.tempKey());

		// 기존 initial과 다르면 업데이트(내부에서 기존 전부 false로 바꾸고, 새 것만 true)
		if (requested != wf.getInitialStatus()) {
			wf.updateInitialStatus(requested);
		}
		return requested;
	}

	// [7] 삭제 대상 계산(요청 목록에 없는 기존 상태들)
	private Set<WorkflowStatus> findStatusesToDelete(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StatusCmd> statuses
	) {
		Set<Long> keepIds = statuses.stream()
			.map(ReplaceWorkflowGraphCommand.StatusCmd::id)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		return wf.getStatuses().stream()
			.filter(s -> !s.isArchived())
			.filter(s -> s.getId() != null && !keepIds.contains(s.getId()))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	// "id:##" 또는 "tmp:UUID" 키로 실제 상태를 얻는 유틸
	// - 클라가 실수로 "UUID"만 보내도 "tmp:UUID"로 표준화해서 찾게 함
	private WorkflowStatus resolveStatus(Map<String, WorkflowStatus> ref, String rawKey) {
		String k = (rawKey.startsWith("id:") || rawKey.startsWith("tmp:")) ? rawKey : ("tmp:" + rawKey);
		WorkflowStatus s = ref.get(k);
		if (s == null)
			throw new InvalidOperationException("Status not found for key: " + rawKey);
		return s;
	}

	// @Transactional
	// public WorkflowResponse patch(PatchWorkflowCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.id());
	//
	// 	// TODO: Workflow의 label/description 수정
	// 	//   - Label 중복 검증 로직 필요(WorkflowValidator에 만들기, Workspace 스코프 내에서 유일해야 함)
	//
	// 	if (!workflow.getLabel().equals(cmd.label())) {
	// 		validator.ensureUniqueLabel(ws, cmd.label());
	// 		Patchers.apply(cmd.label(), workflow::rename);
	// 	}
	//
	// 	Patchers.apply(cmd.description(), workflow::updateDescription);
	// 	Patchers.apply(cmd.color(), workflow::updateColor);
	//
	// 	return WorkflowResponse.from(workflow);
	// }
	//
	// @Transactional
	// public WorkflowResponse softDelete(DeleteWorkflowCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.id());
	//
	// 	// TODO: Workflow 삭제(soft-delete)
	// 	//  - 하나 이상의 IssueType이 Workflow를 사용 중이면 막기(ensureDeletable 구현)
	// 	//  - 이미 엔티티에서 soft-delete가 WorkflowStatus, WorkflowTransition에 전파되도록 구현
	// 	//  - systemType(기본으로 제공하는 Workflow에 해당하면)이면 삭제를 막을까?
	// 	// workflowValidator.ensureDeletable();
	//
	// 	workflow.softDelete();
	//
	// 	return WorkflowResponse.from(workflow);
	// }
	//
	// @Transactional
	// public WorkflowResponse patchStatus(PatchStatusCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());
	// 	WorkflowStatus status = workflowFinder.findWorkflowStatus(workflow, cmd.statusId());
	//
	// 	Patchers.apply(cmd.label(), l -> workflow.renameStatus(status, l));
	// 	Patchers.apply(cmd.description(), d -> workflow.updateStatusDescription(status, d));
	// 	Patchers.apply(cmd.color(), c -> workflow.updateStatusColor(status, c));
	//
	// 	return WorkflowResponse.from(workflow);
	// }
	//
	// @Transactional
	// public WorkflowResponse patchTransition(PatchTransitionCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());
	// 	WorkflowTransition transition = workflowFinder.findWorkflowTransition(workflow, cmd.transitionId());
	//
	// 	Patchers.apply(cmd.label(), l -> workflow.renameTransition(transition, l));
	// 	Patchers.apply(cmd.description(), d -> workflow.updateTransitionDescription(transition, d));
	// 	Patchers.apply(cmd.color(), c -> workflow.updateTransitionColor(transition, c));
	//
	// 	return WorkflowResponse.from(workflow);
	// }
}
