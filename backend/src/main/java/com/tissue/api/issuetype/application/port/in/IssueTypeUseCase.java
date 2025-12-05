package com.tissue.api.issuetype.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.request.DeleteIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.request.RenameIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.response.IssueTypeResponse;

@Transactional
public interface IssueTypeUseCase {

	// REQUIRES_PROJECT_MEMBER
	IssueTypeResponse create(CreateIssueTypeCommand cmd);

	// REQUIRES_PROJECT_ADMIN + OR + REQUIRES_ISSUE_TYPE_CREATOR
	void rename(RenameIssueTypeCommand cmd);

	// REQUIRES_PROJECT_ADMIN + OR + REQUIRES_ISSUE_TYPE_CREATOR
	void update(PatchIssueTypeCommand cmd);

	// REQUIRES_PROJECT_ADMIN
	// TODO: 이걸 archive로 변경 고려
	void delete(DeleteIssueTypeCommand cmd);

	// REQUIRES_PROJECT_ADMIN
	// void archive(ArchiveIssueTypeCommand cmd);
}
