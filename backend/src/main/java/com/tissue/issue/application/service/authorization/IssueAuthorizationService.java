package com.tissue.issue.application.service.authorization;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.exception.InsufficientIssuePermissionException;
import com.tissue.issue.domain.exception.IssueParticipantManageNotAllowedException;
import com.tissue.issue.domain.exception.IssueReviewerManageNotAllowedException;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;
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
        throw new InsufficientIssuePermissionException(workspaceKey, projectKey, issueKey);
    }

    public void requireIssueDeletePermission(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (canDelete(workspaceKey, projectKey, issueKey, memberId)) {
            return;
        }
        throw new InsufficientIssuePermissionException(workspaceKey, projectKey, issueKey);
    }

    public void requireReviewerManagePermission(
            String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (canManageReviewers(workspaceKey, projectKey, issueKey, memberId)) {
            return;
        }
        throw new IssueReviewerManageNotAllowedException(workspaceKey, projectKey, issueKey);
    }

    public void requireParticipantManagePermission(
            String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (canManageParticipants(workspaceKey, projectKey, issueKey, memberId)) {
            return;
        }
        throw new IssueParticipantManageNotAllowedException(workspaceKey, projectKey, issueKey);
    }

    private boolean canManageReviewers(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        return canEdit(workspaceKey, projectKey, issueKey, memberId);
    }

    private boolean canManageParticipants(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        return canDelete(workspaceKey, projectKey, issueKey, memberId);
    }

    // TODO: consider using the Issue entity as the parameter instead of querying the DB
    //  check Author or Assignee in memory
    private boolean canEdit(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (projectAuthorizationService.isAdmin(workspaceKey, projectKey, memberId)) {
            return true;
        }
        return issueQueryRepository.isAuthorOrAssignee(workspaceKey, issueKey, memberId);
    }

    // TODO: consider using the Issue entity as the parameter instead of querying the DB
    private boolean canDelete(String workspaceKey, String projectKey, String issueKey, Long memberId) {
        if (projectAuthorizationService.isAdmin(workspaceKey, projectKey, memberId)) {
            return true;
        }
        return issueQueryRepository.isAuthor(workspaceKey, issueKey, memberId);
    }
}
