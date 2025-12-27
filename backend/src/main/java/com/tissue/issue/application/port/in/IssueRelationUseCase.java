package com.tissue.issue.application.port.in;

import static com.tissue.issue.application.service.authorization.IssueAuthExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.application.dto.request.RemoveIssueRelationCommand;

public interface IssueRelationUseCase {

	// TODO: 관계를 형성하는 타켓 이슈가 다른 프로젝트에 존재하는 경우 권한을 어떻게 처리할까?
	@PreAuthorize(REQUIRES_ISSUE_EDIT_PERMISSION)
	void add(AddIssueRelationCommand cmd);

	@PreAuthorize(REQUIRES_ISSUE_EDIT_PERMISSION)
	void remove(RemoveIssueRelationCommand cmd);
}
