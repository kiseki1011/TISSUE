package com.tissue.workflow.domain.guard;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface TransitionGuard {

	void evaluate(GuardContext context);

	GuardType getType();

	void validateParams(Map<String, Object> params);

	default List<GuardParamMetaData> getParamMetaData() {
		return Collections.emptyList();
	}
}
