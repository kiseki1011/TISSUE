package com.tissue.feature.notification.application.service;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationTargetService {

    private final MemberQueryRepository memberQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueQueryRepository issueQueryRepository;

    /**
     * Retrieve all members in the project as notification targets.
     */
    public List<MemberContactInfo> getAllProjectMembers(String projectKey) {
        return projectMemberQueryRepository.findAllContactsByProjectKey(projectKey);
    }

    /**
     * Retrieve all members in the project as notification targets, excluding a specific project member.
     */
    public List<MemberContactInfo> getProjectMembersExcluding(String projectKey, Long excludedMemberId) {
        return projectMemberQueryRepository.findAllContactsByProjectKeyExcluding(projectKey, excludedMemberId);
    }

    /**
     * Retrieve a specific member as a notification target.
     */
    public Set<MemberContactInfo> getSpecificMemberTarget(Long memberId) {
        return memberQueryRepository.findContactById(memberId).map(Set::of).orElse(Set.of());
    }

    /**
     * Retrieve specific members as notification targets.
     */
    public Set<MemberContactInfo> getSpecificMembersTargets(Set<Long> memberIds) {
        return new HashSet<>(memberQueryRepository.findAllContactsByIdIn(memberIds));
    }

    public List<MemberContactInfo> getMembersByUsernames(Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(memberQueryRepository.findAllContactsByUsernameIn(usernames));
    }

    public Set<MemberContactInfo> getIssueAssignee(String issueKey) {
        Set<MemberContactInfo> result = new HashSet<>();
        issueQueryRepository
                .findAssigneeMemberId(issueKey)
                .flatMap(memberQueryRepository::findContactById)
                .ifPresent(result::add);
        return result;
    }

    public List<MemberContactInfo> getIssueReviewers(String issueKey) {
        Set<Long> reviewerIds = issueQueryRepository.findReviewerMemberIds(issueKey);
        return new ArrayList<>(memberQueryRepository.findAllContactsByIdIn(reviewerIds));
    }

    public List<MemberContactInfo> getIssueSubscribers(String issueKey) {
        Set<Long> subscriberIds = issueQueryRepository.findSubscriberMemberIds(issueKey);
        return new ArrayList<>(memberQueryRepository.findAllContactsByIdIn(subscriberIds));
    }

    public Set<MemberContactInfo> getIssueAssigneeAndReporter(String issueKey) {
        Set<Long> ids = new HashSet<>();
        issueQueryRepository.findAuthorId(issueKey).ifPresent(ids::add);
        issueQueryRepository.findAssigneeMemberId(issueKey).ifPresent(ids::add);

        return new HashSet<>(memberQueryRepository.findAllContactsByIdIn(ids));
    }

    /**
     * Retrieve issue author, assignee and subscribers.
     */
    public Set<MemberContactInfo> getIssueParticipants(String issueKey) {
        Set<Long> participantIds = new HashSet<>();

        issueQueryRepository.findAuthorId(issueKey).ifPresent(participantIds::add);
        issueQueryRepository.findAssigneeMemberId(issueKey).ifPresent(participantIds::add);
        participantIds.addAll(issueQueryRepository.findSubscriberMemberIds(issueKey));

        return new HashSet<>(memberQueryRepository.findAllContactsByIdIn(participantIds));
    }

    /**
     * Retrieve issue author, assignee, subscribers and reviewers.
     */
    public Set<MemberContactInfo> getIssueParticipantsAndReviewers(String issueKey) {
        Set<Long> participantIds = new HashSet<>();

        issueQueryRepository.findAuthorId(issueKey).ifPresent(participantIds::add);
        issueQueryRepository.findAssigneeMemberId(issueKey).ifPresent(participantIds::add);
        participantIds.addAll(issueQueryRepository.findSubscriberMemberIds(issueKey));
        participantIds.addAll(issueQueryRepository.findReviewerMemberIds(issueKey));

        return new HashSet<>(memberQueryRepository.findAllContactsByIdIn(participantIds));
    }
}
