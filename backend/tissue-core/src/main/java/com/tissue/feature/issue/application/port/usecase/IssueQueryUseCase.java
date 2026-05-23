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

    IssueBasicInfo getBasic(IssueIdentifier iid, Long memberId);

    IssueCommonDetail getCommonFieldValues(IssueIdentifier iid, Long memberId);

    IssueCustomDetail getCustomFieldValues(IssueIdentifier iid, Long memberId);

    IssueIdentifierResponse getParent(IssueIdentifier iid, Long memberId);

    List<IssueIdentifierResponse> getChildren(IssueIdentifier iid, Long memberId);

    IssueRelationsDetail getRelations(IssueIdentifier iid, Long memberId);

    ParticipantInfo getAuthor(IssueIdentifier iid, Long memberId);

    IssueReviewersDetail getReviewers(IssueIdentifier iid, Long memberId);

    IssueSubscribersDetail getSubscribers(IssueIdentifier iid, Long memberId);

    List<TransitionDetail> getAvailableTransitions(IssueIdentifier iid, Long memberId);

    // TODO: Issue list / search is owned by IssueSearchUseCase (project-scoped)
    //  Workspace-scoped search will be added to IssueSearchUseCase when the
    //  workspace-level issue view is built on the TUI side.
}
