package com.tissue.feature.issue.application.service.authorization;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.exception.IssueDeleteNotAllowedException;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueAuthorizationService {

    public void requireIssueDeletePermission(Issue issue, WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isProjectCreator(issue.getProject(), actor.getMember().getId())) {
            return;
        }
        if (isIssueAuthor(issue, actor.getMember().getId())) {
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
