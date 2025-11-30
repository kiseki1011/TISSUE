package com.tissue.api.issue.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.AssignParentCommand;
import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.api.issue.application.dto.request.RemoveParentCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;

@Transactional
public interface IssueCommandUseCase {

	IssueCommandResult create(CreateIssueCommand cmd);

	IssueCommandResult updateCommonFields(UpdateCommonFieldsCommand cmd);

	IssueCommandResult updateCustomFields(UpdateCustomFieldsCommand cmd);

	IssueCommandResult updateStoryPoint(UpdateStoryPointCommand cmd);

	IssueCommandResult assignParent(AssignParentCommand cmd);

	IssueCommandResult removeParent(RemoveParentCommand cmd);

	IssueCommandResult softDelete(DeleteIssueCommand cmd);

	// TODO: requestReview()
	// TODO: archive()
	// TODO: batchChangeParent()
	// TODO: batchUpdateStoryPoint()
	// TODO: batchSoftDelete()
	// TODO: cloneIssue()
	// TODO: cloneIssueToProject()
}
