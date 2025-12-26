package com.tissue.comment.application.port.in;

public interface CommentQueryUseCase {

	// TODO: getIssueComments
	//  - should i make this a pagination api? or just get all comments of the issue?
	//  - must have author, createdAt, updatedAt, content
	//  - im going to show a placeholder "(comment was deleted)" in the UI if the comment was deleted(soft-delete)

	// TODO: getMyComments
	//  - pagination api
	//  - all the comments i wrote
	//  - must have createdAt, updatedAt, content, issueKey, workspaceKey
}
