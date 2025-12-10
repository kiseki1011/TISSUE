package com.tissue.api.workflow.application.dto.response;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.workflow.domain.Workflow;

public record WorkflowSummary(
	Long id,
	String label,
	String description,
	ColorType color,
	boolean isSystemProvided,
	boolean isArchived
) {
	public static WorkflowSummary from(Workflow wf) {
		return new WorkflowSummary(
			wf.getId(),
			wf.getLabel().toString(),
			wf.getDescription(),
			wf.getColor(),
			wf.isSystemProvided(),
			wf.isArchived()
		);
	}
}
