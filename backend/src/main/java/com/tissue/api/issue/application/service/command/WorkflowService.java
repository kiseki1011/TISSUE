package com.tissue.api.issue.application.service.command;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.issue.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.domain.newmodel.WorkflowDefinition;
import com.tissue.api.issue.domain.newmodel.WorkflowStep;
import com.tissue.api.issue.domain.newmodel.WorkflowTransition;
import com.tissue.api.issue.domain.service.validator.WorkflowValidator;
import com.tissue.api.issue.domain.util.KeyGenerator;
import com.tissue.api.issue.infrastructure.repository.WorkflowRepository;
import com.tissue.api.issue.presentation.controller.dto.response.WorkflowResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkflowRepository workflowRepository;
	private final WorkflowValidator workflowValidator;
	private final EntityManager em;

	@Transactional
	public WorkflowResponse createWorkflow(CreateWorkflowCommand cmd) {

		workflowValidator.validateCommand(cmd);
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());

		try {
			WorkflowDefinition workflow = WorkflowDefinition.builder()
				.workspace(workspace)
				.label(cmd.label())
				.build();
			workflowRepository.saveAndFlush(workflow);

			workflow.setKey(KeyGenerator.generateWorkflowKey(workflow.getId()));

			// Step mapping using tempKey
			Map<String, WorkflowStep> stepMap = new HashMap<>();
			for (CreateWorkflowCommand.StepCommand s : cmd.steps()) {
				WorkflowStep step = WorkflowStep.builder()
					.workflow(workflow)
					.label(s.label())
					.isInitial(s.isInitial())
					.isFinal(s.isFinal())
					.build();
				workflow.addStep(step);
				stepMap.put(s.tempKey(), step);
			}
			em.flush();

			stepMap.values().forEach(step -> step.setKey(KeyGenerator.generateStepKey(step.getId())));

			for (CreateWorkflowCommand.TransitionCommand t : cmd.transitions()) {
				WorkflowStep sourceStep = stepMap.get(t.sourceTempKey());
				WorkflowStep targetStep = stepMap.get(t.targetTempKey());

				WorkflowTransition transition = WorkflowTransition.builder()
					.workflow(workflow)
					.isMainFlow(t.isMainFlow())
					.sourceStep(sourceStep)
					.targetStep(targetStep)
					.label(t.label())
					.build();
				workflow.addTransition(transition);
			}
			em.flush();

			workflow.getTransitions().forEach(t -> t.setKey(KeyGenerator.generateTransitionKey(t.getId())));
			workflow = workflowRepository.saveAndFlush(workflow);

			return WorkflowResponse.from(workflow);

		} catch (DataIntegrityViolationException e) {
			log.info("Duplicate label.", e);
			throw new DuplicateResourceException("Duplicate label is not allowed for workflows or steps.", e);
		}
	}
}
