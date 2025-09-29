package com.tissue.api.issue.workflow.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
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
	private final WorkflowRepository workflowRepository;
	private final WorkflowValidator workflowValidator;

	// TODO: 엔티티 내부에 중복 Label을 불허하도록 검증하는 메서드 추가
	//  - 이때 WorkflowTransition은 중복 Label을 허용할지 고민하고 있음
	// TODO: terminal=true WorkflowStatus에서 나가는 WorkflowTransition을 불허하는 검증 로직이 있는지 확인? (필요함)
	@Transactional
	public WorkflowResponse createWorkflow(CreateWorkflowCommand cmd) {

		workflowValidator.validateCommand(cmd);
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());

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

			// mainCandidates가 비어있는 상황은 어차피 ensureMainFlowSingleLine에서 검증
			workflowValidator.ensureMainFlowSingleLine(workflow, mainFlowCandidates);
			workflow.defineMainFlow(mainFlowCandidates);

			return WorkflowResponse.from(workflow);
		} catch (DataIntegrityViolationException e) {
			log.info("Failed due to duplicate label.", e);
			throw new DuplicateResourceException("Duplicate label is not allowed for workflows or statuses.", e);
		}
	}

	// TODO: Workflow 삭제(soft-delete)
	//  - 하나 이상의 IssueType이 Workflow를 사용 중이면 막기(ensureDeletable 구현)
	//  - 이미 엔티티에서 soft-delete가 WorkflowStatus, WorkflowTransition에 전파되도록 구현
	//  - systemType(기본으로 제공하는 Workflow에 해당하면)이면 삭제를 막을까?
	// TODO: Workflow의 label/description 수정
	//   - Label 중복 검증 로직 필요(WorkflowValidator에 만들기, Workspace 스코프 내에서 유일해야 함)
	// TODO: WorkflowStatus의 label/description 수정
	//   - Label 중복 검증 로직 필요(WorkflowValidator에 만들까? vs 엔티티 내부에 만들까?)
	// TODO: WorkflowTransition의 label/description 수정
	//   - WorkflowTransition의 경우 Label 중복을 허용할까?
	// TODO: WorkflowTransition의 소스/타겟 status 변경
	//  - 연결이 없는 고아 WorkflowStatus가 없도록 해야 함
	//  - mainFlow 검증 필요
	//  - sourceStatus를 변경하거나, targetStatus를 변경하거나 둘중 하나만 허용
	// TODO: WorkflowTransition 삭제(soft-delete)
	//  - 연결이 없는 고아 WorkflowStatus가 없도록 해야 함
	//    - (필요하면 삭제될때의 문제점을 알릴 수 있으면 좋을 것 같음) 예를 들어서, terminal status가 아닌데도 나가는 transition이 없다거나.
	//    또는 initial status가 아닌데도 들어오는 transition이 없다거나, 등...
	//  - mainFlow 검증 필요
	//  - 필요한 경우의 WorkflowStatus의 initial, terminal의 변경
	// TODO: WorkflowTransition을 추가
	//  - 중복 edge(WorkflowTransition) 검증 로직은 엔티티의 addTransition 안에 이미 사용
	//  - mainFlow 검증 필요
	//  - 필요한 경우의 initialStep, terminalStep의 변경
	// TODO: WorkflowStatus를 추가 + WorkflowTransition 추가
	//  - mainFlow 검증 필요
	//  - 필요한 경우의 WorkflowStatus의 initial, terminal의 변경
	// TODO: WorkflowStatus를 삭제(soft-delete) + 해당 WorkflowStatus를 사용하는 WorkflowTransition 삭제(soft-delete)
	//  - mainFlow 검증 필요
	//  - 필요한 경우의 WorkflowStatus의 initial, terminal의 변경
	//  - 내 생각에는 중간의 WorkflowStatus 삭제는 불허하고 initial이거나 terminal인 WorkflowStatus 삭제만 허용.
	//    이때 해당 WorkflowStatus를 사용한 WorkflowTransition의 삭제도 해야할듯

}
