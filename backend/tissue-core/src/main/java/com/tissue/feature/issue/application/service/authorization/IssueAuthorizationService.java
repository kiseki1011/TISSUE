package com.tissue.feature.issue.application.service.authorization;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ISSUE_DELETE_NOT_ALLOWED;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueAuthorizationService {

    public void requireIssueDeletePermission(Issue issue, ProjectMember actor) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (actor.isManager()) {
            return;
        }
        if (isIssueAuthor(issue, actor.getMemberId())) {
            return;
        }
        throw new ForbiddenException(ISSUE_DELETE_NOT_ALLOWED);
    }

    private boolean isIssueAuthor(Issue issue, Long actorMemberId) {
        return issue.isAuthor(actorMemberId);
    }
}
