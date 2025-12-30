package com.tissue.issue.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.REQUIRES_PROJECT_VIEWER;

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
import org.springframework.security.access.prepost.PreAuthorize;

// TODO: use projectKey instead of extracting it from the issueKey
public interface IssueQueryUseCase {

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    IssueBasicInfo getBasic(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    IssueCommonDetail getCommon(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    IssueCustomDetail getCustom(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    IssueIdentifierResponse getParent(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    List<IssueIdentifierResponse> getChildren(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    IssueRelationsDetail getRelations(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    ParticipantInfo getAuthor(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    IssueReviewersDetail getReviewers(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    IssueSubscribersDetail getSubscribers(String workspaceKey, String projectKey, String issueKey);

    @PreAuthorize(REQUIRES_PROJECT_VIEWER)
    List<TransitionDetail> getAvailableTransitions(String workspaceKey, String projectKey, String issueKey);

    // TODO: getParticipants
    //   - assignee, reviewers, reporter, author(creator) 모두
    // TODO: getIssues() - pagination API
    // TODO: getIssuesByState - getIssues()에 통합 가능할까?
    // TODO: getIssuesByStateCategory - getIssues()에 통합 가능할까?
    // TODO: getComments(추후에 Comment 도메인 리팩토링 후 진행)
    // TODO: getHistory(추후에 ActivityLog 도메인 리팩토링 후 진행)
}
