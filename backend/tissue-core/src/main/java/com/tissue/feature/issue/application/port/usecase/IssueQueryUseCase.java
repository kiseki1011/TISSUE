package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import java.util.List;

public interface IssueQueryUseCase {

    IssueBasicInfo getBasic(String issueKey, ProjectMemberContext actorContext);

    IssueCommonDetail getCommon(String issueKey, ProjectMemberContext actorContext);

    IssueCustomDetail getCustom(String issueKey, ProjectMemberContext actorContext);

    IssueIdentifierResponse getParent(String issueKey, ProjectMemberContext actorContext);

    List<IssueIdentifierResponse> getChildren(String issueKey, ProjectMemberContext actorContext);

    IssueRelationsDetail getRelations(String issueKey, ProjectMemberContext actorContext);

    ParticipantInfo getAuthor(String issueKey, ProjectMemberContext actorContext);

    IssueReviewersDetail getReviewers(String issueKey, ProjectMemberContext actorContext);

    IssueSubscribersDetail getSubscribers(String issueKey, ProjectMemberContext actorContext);

    List<TransitionDetail> getAvailableTransitions(String issueKey, ProjectMemberContext actorContext);

    // TODO: getParticipants
    //   - assignee, reviewers, reporter, author(creator) 모두

    // TODO: getIssues() - pagination API

    // TODO: getIssuesByState - getIssues()에 통합 가능할까?

    // TODO: getIssuesByStateCategory - getIssues()에 통합 가능할까?

    // TODO: getComments(추후에 Comment 도메인 리팩토링 후 진행)

    // TODO: getHistory(추후에 ActivityLog 도메인 리팩토링 후 진행)
}
