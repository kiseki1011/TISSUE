package com.tissue.feature.issue.application.port.in;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.project.application.dto.ProjectMemberContext;

public interface IssueRelationUseCase {

    void add(
            String sourceIssueKey,
            String targetProjectKey,
            String targetIssueKey,
            IssueRelationType relationType,
            ProjectMemberContext actorContext);

    void remove(
            String sourceIssueKey, String targetProjectKey, String targetIssueKey, ProjectMemberContext actorContext);
}
