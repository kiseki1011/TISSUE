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

    IssueCommonDetail getCommonFieldValues(IssueIdentifier issueIdentifier, Long memberId);

    IssueCustomDetail getCustomFieldValues(IssueIdentifier issueIdentifier, Long memberId);

    IssueIdentifierResponse getParent(IssueIdentifier issueIdentifier, Long memberId);

    List<IssueIdentifierResponse> getChildren(IssueIdentifier issueIdentifier, Long memberId);

    IssueRelationsDetail getRelations(IssueIdentifier issueIdentifier, Long memberId);

    ParticipantInfo getAuthor(IssueIdentifier issueIdentifier, Long memberId);

    IssueReviewersDetail getReviewers(IssueIdentifier issueIdentifier, Long memberId);

    IssueSubscribersDetail getSubscribers(IssueIdentifier issueIdentifier, Long memberId);

    List<TransitionDetail> getAvailableTransitions(IssueIdentifier issueIdentifier, Long memberId);

    // TODO: getComments

    // TODO: getHistory

    // TODO: getIssuesByProject - consider separating to a dedicated usecase (example: IssueSearchUseCase)
    //  - paging API (project scoped)
    //  - must be able to search issues by multiple conditions
    //  - default: current Sprint issues + highest priority + nearest dueDate + ACTIVE and INITIAL + highest storyPoint
    //  - condition
    //    - IssuePriority
    //    - dueAt (by period)
    //    - startedAt (by period)
    //    - resolvedAt (by period)
    //    - by current Sprint
    //    - Sprint number
    //    - progress scope (example: find issues with progress between 0 ~ 50%)
    //    - currentState (WorkflowState) (but this is dynamic, should consider separating to a dedicated API)
    //    - StateCategory(INITIAL, ACTIVE, COMPLETED) of currentState
    //    - Tag (search by multiple tags)
    //    - by ProjectMember?
    //      - find issues that a specific ProjectMember is an assignee(or reviewer) for
    //      - should i consider separating to a dedicated API
    //  - sort by
    //    - storyPoint
    //    - dueAt
    //    - startedAt
    //    - resolvedAt
    //    - IssuePriority
    //  - keyword search
    //    - issue key
    //    - title
    //    - content

    // TODO: getIssuesByWorkspace
    //  - similar to getIssuesByProject
    //  - consider separating to a dedicated usecase (example: IssueSearchUseCase)
}
