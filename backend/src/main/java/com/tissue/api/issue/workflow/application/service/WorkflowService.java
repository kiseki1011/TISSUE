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
	// TODO: terminal WorkflowStatus에서 나가는 WorkflowTransition을 불허하는 검증 로직이 있는지 확인? (필요함)
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

			List<WorkflowTransition> mainFlowPath = new ArrayList<>();

			for (CreateWorkflowCommand.TransitionCommand t : cmd.transitions()) {
				WorkflowStatus sourceStatus = statusMap.get(t.sourceTempKey());
				WorkflowStatus targetStatus = statusMap.get(t.targetTempKey());

				WorkflowTransition transition = workflow.addTransition(t.label(), t.description(), sourceStatus,
					targetStatus, t.mainFlow());

				if (t.mainFlow()) {
					mainFlowPath.add(transition);
				}
			}

			// TODO: mainFlowPatch 비어있지 않고 유효한 transition들로 이루어져 있을 것이라고 신뢰
			//  - 그러기 위해서는 유효한 검증 로직으로 사전에 검증이 필요함
			workflowValidator.ensureMainFlowSingleLine(workflow, mainFlowPath);
			workflow.defineMainFlow(mainFlowPath);

			return WorkflowResponse.from(workflow);
		} catch (DataIntegrityViolationException e) {
			log.info("Failed due to duplicate label.", e);
			throw new DuplicateResourceException("Duplicate label is not allowed for workflows or statuses.", e);
		}
	}

	// TODO: WorkflowStatus의 label/description 수정
	// TODO: WorkflowTransition의 label/description 수정
	// TODO: WorkflowTransition의 소스/타겟 status 변경
	//  - 연결이 없는 고아 WorkflowStatus가 없도록 해야 함
	//  - mainFlow 검증 필요
	//  - sourceStatus를 변경하거나, targetStatus를 변경하거나 둘중 하나만 허용
	// TODO: WorkflowTransition 삭제
	//  - 연결이 없는 고아 WorkflowStatus가 없도록 해야 함
	//  - mainFlow 검증 필요
	//  - 필요한 경우의 WorkflowStatus의 initial, terminal의 변경
	// TODO: WorkflowTransition을 추가
	//  - 중복 edge(WorkflowTransition) 검증 로직은 엔티티의 addTransition 안에 이미 사용
	//  - mainFlow 검증 필요
	//  - 필요한 경우의 initialStep, terminalStep의 변경
	// TODO: WorkflowStatus를 추가 + WorkflowTransition 추가
	//  - mainFlow 검증 필요
	//  - 필요한 경우의 WorkflowStatus의 initial, terminal의 변경
}
