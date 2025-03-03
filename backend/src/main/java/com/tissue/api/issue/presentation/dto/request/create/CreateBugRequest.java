package com.tissue.api.issue.presentation.dto.request.create;

import java.util.Set;

import com.tissue.api.common.validator.annotation.size.text.ContentText;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateBugRequest(

	@Valid
	CommonIssueFields common,

	@Min(value = 0, message = "{valid.storypoint.min}")
	@Max(value = 100, message = "{valid.storypoint.max}")
	Integer storyPoint,

	@ContentText
	@NotBlank(message = "{valid.notblank}")
	String reproducingSteps,

	BugSeverity severity,
	Set<String> affectedVersions

) implements CreateIssueRequest {

	@Override
	public IssueType getType() {
		return IssueType.BUG;
	}

	@Override
	public Issue to(Workspace workspace, Issue parentIssue) {
		return Bug.builder()
			.workspace(workspace)
			.title(common.title())
			.content(common.content())
			.summary(common.summary())
			.priority(common.priority())
			.dueAt(common.dueAt())
			.storyPoint(storyPoint)
			.parentIssue(parentIssue)
			.reproducingSteps(reproducingSteps)
			.severity(severity)
			.affectedVersions(affectedVersions)
			.build();
	}
}
