package com.tissue.issue.application.service.authorization;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueDeleteNotAllowedException;
import com.tissue.issue.domain.exception.IssueParticipantManageNotAllowedException;
import com.tissue.issue.domain.exception.IssueReviewerManageNotAllowedException;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.domain.Project;
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

    private boolean isProjectCreator(Project project, Long actorMemberId) {
        return project.getCreatedBy().equals(actorMemberId);
    }
}
