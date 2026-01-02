package com.tissue.issue.application.service.authorization;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.domain.enums.ProjectRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueAuthorizationService {

    private final IssueQueryRepository issueQueryRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public void requireIssueEditPermission(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (canEdit(workspaceKey, projectKey, issueKey, memberId)) {
            return;
        }
        throw new AccessDeniedException(
                "Requires project %s or is the author/assignee of the issue".formatted(ProjectRole.ADMIN.name()));
    }

    public void requireIssueDeletePermission(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (canDelete(workspaceKey, projectKey, issueKey, memberId)) {
            return;
        }
        throw new AccessDeniedException(
                "Requires project %s or is the author of the issue".formatted(ProjectRole.ADMIN.name()));
    }

    public void requireReviewerManagePermission(
            String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (canManageReviewers(workspaceKey, projectKey, issueKey, memberId)) {
            return;
        }
        throw new AccessDeniedException(
                "Requires project %s or is the author/assignee of the issue".formatted(ProjectRole.ADMIN.name()));
    }

    public void requireParticipantManagePermission(
            String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (canManageParticipants(workspaceKey, projectKey, issueKey, memberId)) {
            return;
        }
        throw new AccessDeniedException(
                "Requires project %s or is the author of the issue".formatted(ProjectRole.ADMIN.name()));
    }

    private boolean canManageReviewers(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        return canEdit(workspaceKey, projectKey, issueKey, memberId);
    }

    private boolean canManageParticipants(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        return canDelete(workspaceKey, projectKey, issueKey, memberId);
    }

    // TODO: should i change it to just find the issue entity? the issue entity needs to be retrieved from the DB
    // anyway,
    //  and since were in the same transaction the application service logic can use the persistence-context
    //  so another DB call wont happen
    private boolean canEdit(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (projectAuthorizationService.isAdmin(workspaceKey, projectKey, memberId)) {
            return true;
        }
        return issueQueryRepository.isAuthorOrAssignee(workspaceKey, issueKey, memberId);
    }

    private boolean canDelete(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (projectAuthorizationService.isAdmin(workspaceKey, projectKey, memberId)) {
            return true;
        }
        return issueQueryRepository.isAuthor(workspaceKey, issueKey, memberId);
    }
}
