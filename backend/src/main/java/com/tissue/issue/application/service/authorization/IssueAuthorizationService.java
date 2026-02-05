package com.tissue.issue.application.service.authorization;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.InsufficientIssuePermissionException;
import com.tissue.issue.domain.exception.IssueParticipantManageNotAllowedException;
import com.tissue.issue.domain.exception.IssueReviewerManageNotAllowedException;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueAuthorizationService {

    public void requireIssueEditPermission(Issue issue, ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isIssueAuthor(issue, actor.memberId()) || isIssueAssignee(issue, actor.projectMemberId())) {
            return;
        }
        throw new InsufficientIssuePermissionException(issue);
    }

    public void requireIssueDeletePermission(Issue issue, ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isIssueAuthor(issue, actor.memberId())) {
            return;
        }
        throw new InsufficientIssuePermissionException(issue);
    }

    public void requireReviewerManagePermission(Issue issue, ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isIssueAuthor(issue, actor.memberId()) || isIssueAssignee(issue, actor.projectMemberId())) {
            return;
        }
        throw new IssueReviewerManageNotAllowedException(issue);
    }

    public void requireParticipantManagePermission(Issue issue, ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isIssueAuthor(issue, actor.memberId())) {
            return;
        }
        throw new IssueParticipantManageNotAllowedException(issue);
    }

    private boolean isIssueAuthor(Issue issue, Long actorMemberId) {
        return issue.isAuthor(actorMemberId);
    }

    private boolean isIssueAssignee(Issue issue, Long actorProjectMemberId) {
        return issue.isAssignee(actorProjectMemberId);
    }
}
