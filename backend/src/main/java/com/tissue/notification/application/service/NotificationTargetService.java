package com.tissue.notification.application.service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberContactInfo;
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

    /**
     * Retrieve all members in the workspace as notification targets.
     */
    public List<WorkspaceMemberContactInfo> getAllWorkspaceMembers(String workspaceKey) {
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKey(workspaceKey);
    }

    /**
     * Retrieve all members in the workspace as notification targets, excluding a specific workspace member.
     */
    public List<WorkspaceMemberContactInfo> getAllWorkspaceMembersExcluding(
            String workspaceKey, Long excludedMemberId) {
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyExcluding(workspaceKey, excludedMemberId);
    }

    /**
     * Retrieve all members in the project as notification targets.
     */
    public List<WorkspaceMemberContactInfo> getAllProjectMembers(String workspaceKey, String projectKey) {
        return projectMemberQueryRepository.findAllContactsByProjectKey(workspaceKey, projectKey);
    }

    /**
     * Retrieve all members in the project as notification targets, excluding a specific project member.
     */
    public List<WorkspaceMemberContactInfo> getProjectMembersExcluding(
            String workspaceKey, String projectCode, Long excludedMemberId) {
        return projectMemberQueryRepository.findAllContactsByProjectKeyExcluding(
                workspaceKey, projectCode, excludedMemberId);
    }

    /**
     * Retrieve workspace admins and a specific workspace member as notification targets.
     */
    public Set<WorkspaceMemberContactInfo> getAdminAndSpecificMemberTargets(String workspaceKey, Long memberId) {

        Set<WorkspaceMemberContactInfo> targets = workspaceMemberQueryRepository.findAdminContactsByWorkspace_Key(
                workspaceKey, Set.of(WorkspaceRole.ADMIN, WorkspaceRole.OWNER));

        workspaceMemberQueryRepository
                .findContactByMemberIdAndWorkspaceKey(memberId, workspaceKey)
                .ifPresent(targets::add);

        return targets;
    }

    /**
     * Retrieve a specific workspace member as a notification target.
     */
    public Set<WorkspaceMemberContactInfo> getSpecificMemberTarget(String workspaceKey, Long memberId) {

        Set<WorkspaceMemberContactInfo> target = new HashSet<>();

        workspaceMemberQueryRepository
                .findContactByMemberIdAndWorkspaceKey(memberId, workspaceKey)
                .ifPresent(target::add);

        return target;
    }

    /**
     * Retrieve specific members as notification targets.
     */
    public Set<WorkspaceMemberContactInfo> getSpecificMembersTargets(String workspaceKey, Set<Long> memberIds) {
        return new HashSet<>(
                workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndMemberIds(workspaceKey, memberIds));
    }

    public List<WorkspaceMemberContactInfo> getMembersByUsernames(String workspaceKey, Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndUsernames(workspaceKey, usernames);
    }

    public Set<WorkspaceMemberContactInfo> getIssueAssignee(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContactInfo> target = new HashSet<>();
        issueQueryRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(target::add);
        return target;
    }

    public List<WorkspaceMemberContactInfo> getIssueReviewers(String workspaceKey, String issueKey) {
        return issueQueryRepository.findReviewerContacts(workspaceKey, issueKey);
    }

    public List<WorkspaceMemberContactInfo> getIssueSubscribers(String workspaceKey, String issueKey) {
        return issueQueryRepository.findSubscriberContacts(workspaceKey, issueKey);
    }

    public Set<WorkspaceMemberContactInfo> getIssueAssigneeAndReporter(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContactInfo> targets = new HashSet<>();
        issueQueryRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(targets::add);
        return targets;
    }

    // TODO: Needs refactoring and optimization
    /**
     * Retrieve issue author, assignee and subscribers.
     */
    public Set<WorkspaceMemberContactInfo> getIssueParticipants(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContactInfo> targets = new HashSet<>();
        issueQueryRepository.findAuthorContact(workspaceKey, issueKey).ifPresent(targets::add);
        issueQueryRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(targets::add);
        targets.addAll(issueQueryRepository.findSubscriberContacts(workspaceKey, issueKey));
        return targets;
    }

    // TODO: Needs refactoring and optimization
    /**
     * Retrieve issue author, assignee, subscribers reviewers.
     */
    public Set<WorkspaceMemberContactInfo> getIssueParticipantsAndReviewers(String workspaceKey, String issueKey) {
        Set<WorkspaceMemberContactInfo> targets = new HashSet<>();
        issueQueryRepository.findAuthorContact(workspaceKey, issueKey).ifPresent(targets::add);
        issueQueryRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(targets::add);
        targets.addAll(issueQueryRepository.findSubscriberContacts(workspaceKey, issueKey));
        targets.addAll(issueQueryRepository.findReviewerContacts(workspaceKey, issueKey));
        return targets;
    }

    public List<WorkspaceMemberContactInfo> getWorkspaceAdmins(String workspaceKey) {
        return List.copyOf(workspaceMemberQueryRepository.findAdminContactsByWorkspace_Key(
                workspaceKey, Set.of(WorkspaceRole.ADMIN, WorkspaceRole.OWNER)));
    }
}
