package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.dto.IssueIdentifier;

public interface IssueRelationUseCase {

    void add(
            IssueIdentifier sourceIssueIdentifier,
            String targetIssueKey,
            IssueRelationType relationType,
            Long actorMemberId);

    void remove(IssueIdentifier sourceIssueIdentifier, String targetIssueKey, Long actorMemberId);
}
