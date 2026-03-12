package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;

public interface IssueParticipantUseCase {

    void assign(IssueIdentifier iid, Long targetMemberId, Long actorMemberId);

    void unassign(IssueIdentifier iid, Long actorMemberId);

    void subscribe(IssueIdentifier iid, Long actorMemberId);

    void unsubscribe(IssueIdentifier iid, Long actorMemberId);

    void addReviewer(IssueIdentifier iid, Long targetMemberId, Long actorMemberId);

    void removeReviewer(IssueIdentifier iid, Long targetMemberId, Long actorMemberId);
}
