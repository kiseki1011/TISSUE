package com.tissue.api.issuetype.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issuetype.application.dto.request.AddOptionCommand;
import com.tissue.api.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.request.DeleteIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.request.DeleteOptionCommand;
import com.tissue.api.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.request.RenameIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.request.RenameOptionCommand;
import com.tissue.api.issuetype.application.dto.request.ReorderOptionsCommand;
import com.tissue.api.issuetype.application.dto.response.IssueFieldResponse;

@Transactional
public interface IssueFieldUseCase {

	// REQUIRES_ISSUE_TYPE_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	IssueFieldResponse create(CreateIssueFieldCommand cmd);

	// REQUIRES_ISSUE_FIELD_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	void rename(RenameIssueFieldCommand cmd);

	// REQUIRES_ISSUE_FIELD_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	void update(PatchIssueFieldCommand cmd);

	// REQUIRES_ISSUE_FIELD_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	void delete(DeleteIssueFieldCommand cmd);

	// void archive(ArchiveIssueFieldCommand cmd);

	// REQUIRES_ISSUE_FIELD_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	IssueFieldResponse addOption(AddOptionCommand cmd);

	// REQUIRES_ISSUE_FIELD_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	void renameOption(RenameOptionCommand cmd);

	// REQUIRES_ISSUE_FIELD_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	// TODO: ReorderOptionsResponse
	void reorderOptions(ReorderOptionsCommand cmd);

	// REQUIRES_ISSUE_FIELD_CREATOR + OR + REQUIRES_PROJECT_ADMIN
	void deleteOption(DeleteOptionCommand cmd);
}
