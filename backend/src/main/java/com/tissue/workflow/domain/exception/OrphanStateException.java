package com.tissue.workflow.domain.exception;

import java.util.Collection;

import com.tissue.common.exception.base.BadRequestException;

public class OrphanStateException extends BadRequestException {

	public OrphanStateException(Collection<String> orphanStates, String initialLabel) {
		super("Unreachable states detected: %s. All states must be reachable from '%s'."
			.formatted(orphanStates, initialLabel));

		addContext("orphanStates", orphanStates);
		addContext("initialState", initialLabel);
	}
}
