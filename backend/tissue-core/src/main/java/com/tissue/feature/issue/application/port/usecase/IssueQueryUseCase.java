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
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;

public interface IssueQueryUseCase {

    IssueBasicInfo getBasic(IssueIdentifier issueIdentifier, Long memberId);

    IssueCommonDetail getCommon(IssueIdentifier issueIdentifier, Long memberId);

    IssueCustomDetail getCustom(IssueIdentifier issueIdentifier, Long memberId);

    IssueIdentifierResponse getParent(IssueIdentifier issueIdentifier, Long memberId);

    List<IssueIdentifierResponse> getChildren(IssueIdentifier issueIdentifier, Long memberId);

    IssueRelationsDetail getRelations(IssueIdentifier issueIdentifier, Long memberId);

    ParticipantInfo getAuthor(IssueIdentifier issueIdentifier, Long memberId);

    IssueReviewersDetail getReviewers(IssueIdentifier issueIdentifier, Long memberId);

    IssueSubscribersDetail getSubscribers(IssueIdentifier issueIdentifier, Long memberId);

    List<TransitionDetail> getAvailableTransitions(IssueIdentifier issueIdentifier, Long memberId);

    // TODO: getParticipants
    //   - assignee, reviewers, reporter, author(creator) 모두

    // TODO: getIssues() - pagination API

    // TODO: getIssuesByState - getIssues()에 통합 가능할까?

    // TODO: getIssuesByStateCategory - getIssues()에 통합 가능할까?

    // TODO: getComments(추후에 Comment 도메인 리팩토링 후 진행)

    // TODO: getHistory(추후에 ActivityLog 도메인 리팩토링 후 진행)
}
