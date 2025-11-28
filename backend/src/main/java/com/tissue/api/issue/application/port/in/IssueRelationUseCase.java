package com.tissue.api.issue.application.port.in;

import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.api.issue.application.dto.response.IssueRelationResult;

@Transactional
public interface IssueRelationUseCase {

	// TODO: 응답을 IssueCommandResult vs IssueRelationResult 중 뭘 사용하는게 좋을까?
	IssueRelationResult add(AddIssueRelationCommand cmd);

	// TODO: RemoveIssueRelationCommand
	void remove(String workspaceKey, String sourceIssueKey, String targetIssueKey);
}
