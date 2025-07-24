package com.tissue.api.issue.application.service.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.domain.newmodel.WorkflowDefinition;
import com.tissue.api.issue.domain.newmodel.WorkflowStep;
import com.tissue.api.issue.domain.newmodel.WorkflowTransition;
import com.tissue.api.issue.domain.util.KeyGenerator;
import com.tissue.api.issue.infrastructure.repository.WorkflowRepository;
import com.tissue.api.issue.presentation.controller.dto.response.WorkflowResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowService {

	private final WorkspaceReader workspaceReader;
	private final WorkflowRepository workflowRepository;
	private final EntityManager em;

	@Transactional
	public WorkflowResponse createWorkflow(CreateWorkflowCommand cmd) {
		Workspace workspace = workspaceReader.findWorkspace(cmd.workspaceCode());

		WorkflowDefinition workflow = WorkflowDefinition.builder()
			.workspace(workspace)
			.label(cmd.label())
			.build();
		workflowRepository.save(workflow);
		em.flush();

		workflow.setKey(KeyGenerator.generateWorkflowKey(workflow.getId()));

		List<WorkflowStep> steps = new ArrayList<>();
		for (CreateWorkflowCommand.StepCommand s : cmd.steps()) {
			WorkflowStep step = WorkflowStep.builder()
				.workflow(workflow)
				.label(s.label())
				.isInitial(s.isInitial())
				.isFinal(s.isFinal())
				.build();
			workflow.addStep(step);
			steps.add(step);
		}
		em.flush();

		steps.forEach(step -> step.setKey(KeyGenerator.generateStepKey(step.getId())));

		// TODO: Doesnt using the index of the list have problems?
		//  For example, lets say the given steps list was this: TODO, IN_PROGRESS, PAUSED, IN_REVIEW, DONE, CANCELLED
		//  The request format is this: (label, isInitial, isFinal)
		//  Lets say the main flow is TODO -> IN_PROGRESS -> IN_REVIEW -> DONE
		//  But the following transitions exist too.
		//  (Any Step except for DONE) -> CANCELLED, (Any Step except for DONE, CANCELLED) -> PAUSED
		//  CANCELLED is a final step too.
		//  From my point of view, using the index of the steps list, for the transitions make it kind of hard to follow.
		//  In this case, cant i just use the label of the step or step id or step key? Or is it still better to use index of the list?

		for (CreateWorkflowCommand.TransitionCommand t : cmd.transitions()) {
			WorkflowStep sourceStep = steps.get(t.sourceIndex());
			WorkflowStep targetStep = steps.get(t.targetIndex());

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
	}
}
