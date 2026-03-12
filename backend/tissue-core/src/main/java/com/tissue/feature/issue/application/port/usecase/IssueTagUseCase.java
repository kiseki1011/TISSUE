package com.tissue.feature.issue.application.port.usecase;

import com.tissue.shared.dto.IssueIdentifier;

public interface IssueTagUseCase {

    void addTag(IssueIdentifier iid, Long tagId, Long actorMemberId);

    void removeTag(IssueIdentifier iid, Long tagId, Long actorMemberId);
}
