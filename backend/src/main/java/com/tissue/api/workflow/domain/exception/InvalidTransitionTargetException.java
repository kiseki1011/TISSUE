package com.tissue.api.workflow.domain.exception;

import java.util.Collection;

import com.tissue.api.common.exception.base.BadRequestException;

public class InvalidTransitionTargetException extends BadRequestException {

	public InvalidTransitionTargetException(Collection<String> sourceStateNames, String targetStateName) {
		super("Transitions cannot target the initial (TODO) state. Invalid sources: %s -> [%s]"
			.formatted(sourceStateNames, targetStateName));

		addContext("invalidSourceStates", sourceStateNames);
		addContext("targetState", targetStateName);
	}
}
