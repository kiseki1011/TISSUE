package com.tissue.issuetype.application.port.in;

import static com.tissue.security.authorization.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.issuetype.application.dto.request.AddOptionCommand;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.DeleteIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.DeleteOptionCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.RenameIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.RenameOptionCommand;
import com.tissue.issuetype.application.dto.request.ReorderOptionsCommand;
import com.tissue.issuetype.application.dto.response.IssueFieldResponse;

public interface IssueFieldUseCase {

	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	IssueFieldResponse create(CreateIssueFieldCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	void rename(RenameIssueFieldCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	void update(PatchIssueFieldCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	void delete(DeleteIssueFieldCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	IssueFieldResponse addOption(AddOptionCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	void renameOption(RenameOptionCommand cmd);

	// TODO: should i return the reordered list of options for convenience even though its a command API?
	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	void reorderOptions(ReorderOptionsCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_TYPE_MANAGE)
	void deleteOption(DeleteOptionCommand cmd);
}
