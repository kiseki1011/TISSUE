package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.dto.IssueIdentifier;

public interface IssueRelationUseCase {

    void add(IssueIdentifier sourceIid, String targetIssueKey, IssueRelationType relationType, Long actorMemberId);

    void remove(IssueIdentifier sourceIid, String targetIssueKey, Long actorMemberId);
}
