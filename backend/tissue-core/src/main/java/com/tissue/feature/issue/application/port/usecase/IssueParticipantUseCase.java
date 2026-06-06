package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;

public interface IssueParticipantUseCase {

    void assign(IssueIdentifier iid, Long targetMemberId, Long actorMemberId);

    /**
     * Assign the issue to the calling actor, but only when it is unassigned (or already theirs).
     * Throws an exception when the issue is held by another member.
     */
    void claim(IssueIdentifier iid, Long actorMemberId);

    void unassign(IssueIdentifier iid, Long actorMemberId);

    void subscribe(IssueIdentifier iid, Long actorMemberId);

    void unsubscribe(IssueIdentifier iid, Long actorMemberId);

    void addReviewer(IssueIdentifier iid, Long targetMemberId, Long actorMemberId);

    void removeReviewer(IssueIdentifier iid, Long targetMemberId, Long actorMemberId);
}
