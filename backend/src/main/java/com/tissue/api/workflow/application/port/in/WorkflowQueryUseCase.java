package com.tissue.api.workflow.application.port.in;

import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface WorkflowQueryUseCase {
}
