package com.tissue.notification.application.service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationTargetService {

    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueQueryRepository issueQueryRepository;

    /** Retrieve all members in the workspace as notification targets. */
    public List<WorkspaceMemberContact> getWorkspaceWideMemberTargets(String workspaceCode) {
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKey(workspaceCode);
    }

    /** Retrieve all members in the workspace as notification targets, excluding a specific member. */
    public List<WorkspaceMemberContact> getAllWorkspaceMembersExcluding(String workspaceCode, Long excludedMemberId) {
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyExcluding(workspaceCode, excludedMemberId);
    }

    /** Retrieve all members in the project as notification targets. */
    public List<WorkspaceMemberContact> getProjectMemberTargets(String workspaceCode, String projectCode) {
        return projectMemberQueryRepository.findAllContactsByProjectKey(workspaceCode, projectCode);
    }

    /** Retrieve all members in the project as notification targets, excluding a specific member. */
    public List<WorkspaceMemberContact> getProjectMembersExcluding(
            String workspaceCode, String projectCode, Long excludedMemberId) {
        return projectMemberQueryRepository.findAllContactsByProjectKeyExcluding(
                workspaceCode, projectCode, excludedMemberId);
    }

    /** Retrieve workspace administrators and a specific member as notification targets. */
    public Set<WorkspaceMemberContact> getAdminAndSpecificMemberTargets(String workspaceCode, Long memberId) {

        Set<WorkspaceMemberContact> targets = workspaceMemberQueryRepository.findAdminContactsByWorkspace_Key(
                workspaceCode, Set.of(WorkspaceRole.ADMIN, WorkspaceRole.OWNER));

        workspaceMemberQueryRepository
                .findContactByMemberIdAndWorkspaceKey(memberId, workspaceCode)
                .ifPresent(targets::add);

        return targets;
    }

    /** Retrieve a specific member as a notification target. */
    public Set<WorkspaceMemberContact> getSpecificMemberTarget(String workspaceCode, Long memberId) {

        Set<WorkspaceMemberContact> target = new HashSet<>();

        workspaceMemberQueryRepository
                .findContactByMemberIdAndWorkspaceKey(memberId, workspaceCode)
                .ifPresent(target::add);

        return target;
    }

    /** Retrieve specific members as notification targets. */
    public Set<WorkspaceMemberContact> getSpecificMembersTargets(String workspaceCode, Set<Long> memberIds) {
        return new HashSet<>(
                workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndMemberIds(workspaceCode, memberIds));
    }

    public Set<WorkspaceMemberContact> getIssueAssignee(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContact> target = new HashSet<>();
        issueQueryRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(target::add);
        return target;
    }

    public Set<WorkspaceMemberContact> getIssueReporter(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContact> target = new HashSet<>();
        issueQueryRepository.findReporterContact(workspaceKey, issueKey).ifPresent(target::add);
        return target;
    }

    public List<WorkspaceMemberContact> getIssueReviewers(String workspaceKey, String issueKey) {
        return issueQueryRepository.findReviewerContacts(workspaceKey, issueKey);
    }

    public List<WorkspaceMemberContact> getIssueSubscribers(String workspaceKey, String issueKey) {
        return issueQueryRepository.findSubscriberContacts(workspaceKey, issueKey);
    }

    public Set<WorkspaceMemberContact> getIssueAssigneeAndReporter(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContact> targets = new HashSet<>();
        issueQueryRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(targets::add);
        issueQueryRepository.findReporterContact(workspaceKey, issueKey).ifPresent(targets::add);
        return targets;
    }

    /**
     * Retrieve Author, Assignee, Reporter, and Subscribers.
     * Useful for general updates.
     */
    public Set<WorkspaceMemberContact> getIssueParticipants(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContact> targets = new HashSet<>();
        issueQueryRepository.findAuthorContact(workspaceKey, issueKey).ifPresent(targets::add);
        issueQueryRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(targets::add);
        issueQueryRepository.findReporterContact(workspaceKey, issueKey).ifPresent(targets::add);
        targets.addAll(issueQueryRepository.findSubscriberContacts(workspaceKey, issueKey));
        return targets;
    }

    /**
     * Retrieve Author, Assignee, Reporter, Subscribers, and Reviewers.
     * Useful for status transitions or deletion.
     */
    public Set<WorkspaceMemberContact> getIssueParticipantsAndReviewers(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContact> targets = getIssueParticipants(workspaceKey, issueKey);
        targets.addAll(issueQueryRepository.findReviewerContacts(workspaceKey, issueKey));
        return targets;
    }
}
