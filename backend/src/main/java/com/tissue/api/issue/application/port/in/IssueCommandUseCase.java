package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueResult;

public interface IssueCommandUseCase {
	IssueResult create(CreateIssueCommand cmd);

	IssueResult updateCommonFields(UpdateCommonFieldsCommand cmd);

	IssueResult updateCustomFields(UpdateCustomFieldsCommand cmd);

	IssueResult updateStoryPoint(UpdateStoryPointCommand cmd);

	IssueResult assignParent(String workspaceKey, String issueKey, String parentIssueKey);

	IssueResult removeParent(String workspaceKey, String issueKey);

	IssueResult softDelete(String workspaceKey, String issueKey);

	// TODO: requestReview()
	// TODO: batchChangeParent()
	// TODO: batchUpdateStoryPoint()
	// TODO: batchSoftDelete()
	// TODO: cloneIssue()
	// TODO: cloneIssueToProject()
}
