package com.tissue.api.issue.application.port.in;

import java.util.List;

import com.tissue.api.issue.application.dto.response.IssueCommonDetail;
import com.tissue.api.issue.application.dto.response.IssueCustomDetail;
import com.tissue.api.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.api.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.api.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.api.issue.application.dto.response.TransitionDetail;
import com.tissue.api.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.api.issue.application.dto.response.info.IssueIdentificationInfo;
import com.tissue.api.issue.application.dto.response.info.ParticipantInfo;

public interface IssueQueryUseCase {

	IssueBasicInfo getBasic(String workspaceKey, String issueKey);

	IssueCommonDetail getCommon(String workspaceKey, String issueKey);

	IssueCustomDetail getCustom(String workspaceKey, String issueKey);

	IssueIdentificationInfo getParent(String workspaceKey, String issueKey);

	List<IssueIdentificationInfo> getChildren(String workspaceKey, String issueKey);

	IssueRelationsDetail getRelations(String workspaceKey, String issueKey);

	ParticipantInfo getAuthor(String workspaceKey, String issueKey);

	IssueReviewersDetail getReviewers(String workspaceKey, String issueKey);

	IssueSubscribersDetail getSubscribers(String workspaceKey, String issueKey);

	List<TransitionDetail> getAvailableTransitions(String workspaceKey, String issueKey);

	// TODO: getParticipants
	// TODO: getIssuesByState
	// TODO: getIssuesByStateCategory
	// TODO: getIssues()
	// TODO: getComments
	// TODO: getHistory
}
