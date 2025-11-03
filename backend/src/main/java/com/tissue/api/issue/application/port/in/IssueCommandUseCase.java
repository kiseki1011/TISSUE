package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;

public interface IssueCommandUseCase {
	IssueCommandResult create(CreateIssueCommand cmd);

	IssueCommandResult updateCommonFields(UpdateCommonFieldsCommand cmd);

	IssueCommandResult updateCustomFields(UpdateCustomFieldsCommand cmd);

	IssueCommandResult updateStoryPoint(UpdateStoryPointCommand cmd);

	IssueCommandResult assignParent(String workspaceKey, String issueKey, String parentIssueKey);

	IssueCommandResult removeParent(String workspaceKey, String issueKey);

	IssueCommandResult softDelete(String workspaceKey, String issueKey);

	// TODO: requestReview()
	// TODO: batchChangeParent()
	// TODO: batchUpdateStoryPoint()
	// TODO: batchSoftDelete()
	// TODO: cloneIssue()
	// TODO: cloneIssueToProject()
}
