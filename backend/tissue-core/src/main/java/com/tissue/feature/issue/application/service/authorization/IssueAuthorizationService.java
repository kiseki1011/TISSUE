package com.tissue.feature.issue.application.service.authorization;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.exception.IssueDeleteNotAllowedException;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueAuthorizationService {

    public void requireIssueDeletePermission(Issue issue, ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isProjectCreator(issue.getProject(), actor.memberId())) {
            return;
        }
        if (isIssueAuthor(issue, actor.memberId())) {
            return;
        }
        throw new IssueDeleteNotAllowedException(issue.getKey());
    }

    private boolean isIssueAuthor(Issue issue, Long actorMemberId) {
        return issue.isAuthor(actorMemberId);
    }

    private boolean isProjectCreator(Project project, Long actorMemberId) {
        return project.getCreatedBy().equals(actorMemberId);
    }
}
