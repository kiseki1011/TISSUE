package com.tissue.api.workflow.domain.guard;

public interface TransitionGuard {

	boolean evaluate(GuardContext context);

	String getFailureMessage(GuardContext context);

	GuardType getType();
}
