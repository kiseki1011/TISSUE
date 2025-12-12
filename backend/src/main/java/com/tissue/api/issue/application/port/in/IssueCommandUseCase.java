package com.tissue.api.issue.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.api.issue.application.dto.request.AssignParentCommand;
import com.tissue.api.issue.application.dto.request.CreateIssueCommand;
import com.tissue.api.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.api.issue.application.dto.request.RemoveParentCommand;
import com.tissue.api.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCreateResponse;

public interface IssueCommandUseCase {

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	IssueCreateResponse create(CreateIssueCommand cmd);

	// @PreAuthorize(REQUIRES_PROJECT_ADMIN + OR + IssueSecurityExpressions.REQUIRES_AUTHOR + OR + IssueSecurityExpressions.REQUIRES_ASSIGNEE)
	void updateCommonFields(UpdateCommonFieldsCommand cmd);

	// @PreAuthorize(REQUIRES_PROJECT_ADMIN + OR + IssueSecurityExpressions.REQUIRES_AUTHOR + OR + IssueSecurityExpressions.REQUIRES_ASSIGNEE)
	void updateCustomFields(UpdateCustomFieldsCommand cmd);

	// @PreAuthorize(REQUIRES_PROJECT_ADMIN + OR + IssueSecurityExpressions.REQUIRES_AUTHOR + OR + IssueSecurityExpressions.REQUIRES_ASSIGNEE)
	void updateStoryPoint(UpdateStoryPointCommand cmd);

	// @PreAuthorize(REQUIRES_PROJECT_ADMIN + OR + IssueSecurityExpressions.REQUIRES_AUTHOR + OR + IssueSecurityExpressions.REQUIRES_ASSIGNEE)
	void assignParent(AssignParentCommand cmd);

	// @PreAuthorize(REQUIRES_PROJECT_ADMIN + OR + IssueSecurityExpressions.REQUIRES_AUTHOR + OR + IssueSecurityExpressions.REQUIRES_ASSIGNEE)
	void removeParent(RemoveParentCommand cmd);

	// @PreAuthorize(REQUIRES_PROJECT_ADMIN + OR + IssueSecurityExpressions.REQUIRES_AUTHOR)
	void softDelete(DeleteIssueCommand cmd);

	// TODO: restore()
	// TODO: approve()
	//   - reject(), requestChange()도 추가해야 하나?
	// TODO: batchChangeParent()
	// TODO: batchSoftDelete()
	// TODO: cloneIssue()
	//  - 특정 이슈 내용 복사해서 새로 생성? 필요한지는 모르겠네...
	//  - 필요해도 아마 다른 프로젝트로 특정 이슈를 복사하는 것 정도?
}
