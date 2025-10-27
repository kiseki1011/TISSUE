package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueResponse;

public interface IssueCommandUseCase {
	IssueResponse create(CreateIssueCommand cmd);

	IssueResponse updateCommonFields(UpdateCommonFieldsCommand cmd);

	IssueResponse updateCustomFields(UpdateCustomFieldsCommand cmd);

	IssueResponse updateStoryPoint(UpdateStoryPointCommand cmd);

	IssueResponse assignParent(String workspaceKey, String issueKey, String parentIssueKey);

	IssueResponse removeParent(String workspaceKey, String issueKey);

	IssueResponse softDelete(String workspaceKey, String issueKey);

	// TODO: requestReview()
	// TODO: batchChangeParent()
	// TODO: batchUpdateStoryPoint()
	// TODO: batchSoftDelete()
	// TODO: cloneIssue()
	// TODO: cloneIssueToProject()
}
