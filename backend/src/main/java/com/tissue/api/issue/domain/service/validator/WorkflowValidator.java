package com.tissue.api.issue.domain.service.validator;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.application.dto.CreateWorkflowCommand;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	// TODO: Needs validation if main flow of a WorkflowDefinition is a single straight line
	// TODO: I wonder if i can just encapsulate validation logic inside the entities?
	//  (WorkflowDefinition, WorkflowStep, WorkflowTransition) If possible, is it a good design?
	public void validateCommand(CreateWorkflowCommand cmd) {
		Set<String> stepLabels = new HashSet<>();
		int initialCount = 0;
		int finalCount = 0;

		for (CreateWorkflowCommand.StepCommand step : cmd.steps()) {
			if (!stepLabels.add(step.label())) {
				throw new DuplicateResourceException("Duplicate step label: " + step.label());
			}
			if (step.isInitial()) {
				initialCount++;
			}

			if (step.isFinal()) {
				finalCount++;
			}
		}

		if (initialCount != 1) {
			throw new InvalidOperationException("Workflow must have a single initial step.");
		}
		if (finalCount == 0) {
			throw new InvalidOperationException("Workflow must have at least one final step.");
		}
	}
}
