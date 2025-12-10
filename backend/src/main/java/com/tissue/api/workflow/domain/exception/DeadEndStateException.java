package com.tissue.api.workflow.domain.exception;

import java.util.Collection;

import com.tissue.api.common.exception.base.BadRequestException;

public class DeadEndStateException extends BadRequestException {

	public DeadEndStateException(Collection<String> deadEndLabels) {
		super("The following 'IN_PROGRESS' states have no outgoing transitions: %s. ".formatted(deadEndLabels)
			+ "Please connect them to a next state or change their category to 'DONE'.");
		addContext("deadEndStates", deadEndLabels);
	}
}
