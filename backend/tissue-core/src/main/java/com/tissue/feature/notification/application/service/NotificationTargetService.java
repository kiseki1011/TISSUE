package com.tissue.feature.notification.application.service;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberContactInfo;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
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
     * Retrieve a specific workspace member as a notification target.
     */
    public Set<WorkspaceMemberContactInfo> getSpecificMemberTarget(String workspaceKey, Long memberId) {
        return workspaceMemberQueryRepository
                .findContactByMemberIdAndWorkspaceKey(memberId, workspaceKey)
                .map(Set::of)
                .orElse(Set.of());
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
        return issueQueryRepository
                .findAssigneeMemberId(workspaceKey, issueKey)
                .flatMap(id -> workspaceMemberQueryRepository.findContactByMemberIdAndWorkspaceKey(id, workspaceKey))
                .map(Set::of)
                .orElse(Set.of());
    }

    public List<WorkspaceMemberContactInfo> getIssueReviewers(String workspaceKey, String issueKey) {
        Set<Long> reviewerIds = issueQueryRepository.findReviewerMemberIds(workspaceKey, issueKey);
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndMemberIds(workspaceKey, reviewerIds);
    }

    public List<WorkspaceMemberContactInfo> getIssueSubscribers(String workspaceKey, String issueKey) {
        Set<Long> subscriberIds = issueQueryRepository.findSubscriberMemberIds(workspaceKey, issueKey);
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndMemberIds(workspaceKey, subscriberIds);
    }

    public Set<WorkspaceMemberContactInfo> getIssueAssigneeAndReporter(String workspaceKey, String issueKey) {
        Set<Long> ids = new HashSet<>();
        issueQueryRepository.findAuthorId(workspaceKey, issueKey).ifPresent(ids::add);
        issueQueryRepository.findAssigneeMemberId(workspaceKey, issueKey).ifPresent(ids::add);

        return new HashSet<>(
                workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndMemberIds(workspaceKey, ids));
    }

    /**
     * Retrieve issue author, assignee and subscribers.
     */
    public Set<WorkspaceMemberContactInfo> getIssueParticipants(String workspaceKey, String issueKey) {
        Set<Long> participantIds = new HashSet<>();

        issueQueryRepository.findAuthorId(workspaceKey, issueKey).ifPresent(participantIds::add);
        issueQueryRepository.findAssigneeMemberId(workspaceKey, issueKey).ifPresent(participantIds::add);
        participantIds.addAll(issueQueryRepository.findSubscriberMemberIds(workspaceKey, issueKey));

        return new HashSet<>(
                workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndMemberIds(workspaceKey, participantIds));
    }

    /**
     * Retrieve issue author, assignee, subscribers and reviewers.
     */
    public Set<WorkspaceMemberContactInfo> getIssueParticipantsAndReviewers(String workspaceKey, String issueKey) {
        Set<Long> participantIds = new HashSet<>();

        issueQueryRepository.findAuthorId(workspaceKey, issueKey).ifPresent(participantIds::add);
        issueQueryRepository.findAssigneeMemberId(workspaceKey, issueKey).ifPresent(participantIds::add);
        participantIds.addAll(issueQueryRepository.findSubscriberMemberIds(workspaceKey, issueKey));
        participantIds.addAll(issueQueryRepository.findReviewerMemberIds(workspaceKey, issueKey));

        return new HashSet<>(
                workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyAndMemberIds(workspaceKey, participantIds));
    }

    public List<WorkspaceMemberContactInfo> getWorkspaceAdmins(String workspaceKey) {
        return List.copyOf(workspaceMemberQueryRepository.findAdminContactsByWorkspace_Key(
                workspaceKey, Set.of(WorkspaceRole.ADMIN, WorkspaceRole.OWNER)));
    }
}
