package com.tissue.api.issue.application.service.command;

import java.util.ArrayList;
import java.util.List;

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

		// TODO: I wonder if I understand right how the persistance in JPA works
		//  - am i using em.flush() right?
		//  - how exactly does persisting WorkflowStep, WorkflowTransition with cascade work?
		//  - i wonder if i can remove the flush() code by using a KeySequence instead of the entity's id
		try {
			WorkflowDefinition workflow = WorkflowDefinition.builder()
				.workspace(workspace)
				.label(cmd.label())
				.build();
			workflowRepository.save(workflow);
			em.flush();

			workflow.setKey(KeyGenerator.generateWorkflowKey(workflow.getId()));

			List<WorkflowStep> stepsBuffer = new ArrayList<>();

			for (CreateWorkflowCommand.StepCommand s : cmd.steps()) {
				WorkflowStep step = WorkflowStep.builder()
					.workflow(workflow)
					.label(s.label())
					.isInitial(s.isInitial())
					.isFinal(s.isFinal())
					.build();
				workflow.addStep(step);
				stepsBuffer.add(step);
			}
			em.flush();

			stepsBuffer.forEach(step -> step.setKey(KeyGenerator.generateStepKey(step.getId())));

			for (CreateWorkflowCommand.TransitionCommand t : cmd.transitions()) {
				WorkflowStep sourceStep = stepsBuffer.get(t.sourceIndex());
				WorkflowStep targetStep = stepsBuffer.get(t.targetIndex());

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
			workflow = workflowRepository.save(workflow);
			em.flush();

			return WorkflowResponse.from(workflow);

		} catch (DataIntegrityViolationException e) {
			log.info("Duplicate label.", e);
			throw new DuplicateResourceException("Duplicate label is not allowed for workflows or steps.", e);
		}
	}
}
