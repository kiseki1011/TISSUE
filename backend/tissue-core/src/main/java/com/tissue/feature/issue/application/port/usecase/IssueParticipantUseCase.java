package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;

public interface IssueParticipantUseCase {

    void assign(IssueIdentifier issueIdentifier, Long targetMemberId, Long memberId);

    void unassign(IssueIdentifier issueIdentifier, Long memberId);

    void subscribe(IssueIdentifier issueIdentifier, Long memberId);

    void unsubscribe(IssueIdentifier issueIdentifier, Long memberId);

    void addReviewer(IssueIdentifier issueIdentifier, Long targetMemberId, Long memberId);

    void removeReviewer(IssueIdentifier issueIdentifier, Long targetMemberId, Long memberId);
}
