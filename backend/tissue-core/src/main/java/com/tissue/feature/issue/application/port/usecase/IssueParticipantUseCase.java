package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;

public interface IssueParticipantUseCase {

    void assign(IssueIdentifier issueIdentifier, Long targetMemberId, Long actorMemberId);

    void unassign(IssueIdentifier issueIdentifier, Long actorMemberId);

    void subscribe(IssueIdentifier issueIdentifier, Long actorMemberId);

    void unsubscribe(IssueIdentifier issueIdentifier, Long actorMemberId);

    void addReviewer(IssueIdentifier issueIdentifier, Long targetMemberId, Long actorMemberId);

    void removeReviewer(IssueIdentifier issueIdentifier, Long targetMemberId, Long actorMemberId);
}
