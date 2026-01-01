package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.response.IssueCommonDetail;
import com.tissue.issue.application.dto.response.IssueCustomDetail;
import com.tissue.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.issue.application.dto.response.TransitionDetail;
import com.tissue.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.issue.application.dto.response.info.ParticipantInfo;
import java.util.List;

// TODO: use projectKey instead of extracting it from the issueKey
public interface IssueQueryUseCase {

    IssueBasicInfo getBasic(String workspaceKey, String projectKey, String issueKey);

    IssueCommonDetail getCommon(String workspaceKey, String projectKey, String issueKey);

    IssueCustomDetail getCustom(String workspaceKey, String projectKey, String issueKey);

    IssueIdentifierResponse getParent(String workspaceKey, String projectKey, String issueKey);

    List<IssueIdentifierResponse> getChildren(String workspaceKey, String projectKey, String issueKey);

    IssueRelationsDetail getRelations(String workspaceKey, String projectKey, String issueKey);

    ParticipantInfo getAuthor(String workspaceKey, String projectKey, String issueKey);

    IssueReviewersDetail getReviewers(String workspaceKey, String projectKey, String issueKey);

    IssueSubscribersDetail getSubscribers(String workspaceKey, String projectKey, String issueKey);

    List<TransitionDetail> getAvailableTransitions(String workspaceKey, String projectKey, String issueKey);

    // TODO: getParticipants
    //   - assignee, reviewers, reporter, author(creator) 모두
    // TODO: getIssues() - pagination API
    // TODO: getIssuesByState - getIssues()에 통합 가능할까?
    // TODO: getIssuesByStateCategory - getIssues()에 통합 가능할까?
    // TODO: getComments(추후에 Comment 도메인 리팩토링 후 진행)
    // TODO: getHistory(추후에 ActivityLog 도메인 리팩토링 후 진행)
}
