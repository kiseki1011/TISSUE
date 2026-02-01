package com.tissue.issue.adapter.persistence;

import com.tissue.issue.adapter.persistence.repository.IssueJpaRepository;
import com.tissue.issue.application.dto.IssueCountProjection;
import com.tissue.issue.application.dto.IssueCountStats;
import com.tissue.issue.application.dto.IssuePointStats;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssuePersistenceAdapter implements IssueQueryRepository {

    private final IssueJpaRepository issueJpaRepository;

    @Override
    public Optional<Issue> findById(Long id) {
        return issueJpaRepository.findById(id);
    }

    @Override
    public Optional<Issue> findWithBasicInfo(String workspaceKey, String issueKey) {
        return issueJpaRepository.findWithBasicInfo(workspaceKey, issueKey);
    }

    @Override
    public Optional<Issue> findWithDetail(String workspaceKey, String issueKey) {
        return issueJpaRepository.findWithDetail(workspaceKey, issueKey);
    }

    @Override
    public Optional<Issue> findByKeyAndProject(String issueKey, Project project) {
        return issueJpaRepository.findByKeyAndProject(issueKey, project);
    }

    @Override
    public Optional<Issue> findByKeyAndWorkspaceKey(String issueKey, String workspaceKey) {
        return issueJpaRepository.findByKeyAndWorkspaceKey(issueKey, workspaceKey);
    }

    @Override
    public List<Issue> findByKeyInAndWorkspaceKey(Collection<String> issueKeys,
        String workspaceKey) {
        return issueJpaRepository.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
    }

    @Override
    public Optional<Issue> findWithParent(String workspaceKey, String issueKey) {
        return issueJpaRepository.findWithParent(workspaceKey, issueKey);
    }

    @Override
    public List<Issue> findChildren(String workspaceKey, String issueKey) {
        return issueJpaRepository.findChildren(workspaceKey, issueKey);
    }

    @Override
    public boolean hasChildren(String workspaceKey, String issueKey) {
        return issueJpaRepository.hasChildren(workspaceKey, issueKey);
    }

    @Override
    public boolean existsByIssueType(IssueType issueType) {
        return issueJpaRepository.existsByIssueType(issueType);
    }

    @Override
    public Integer sumChildrenStoryPoints(Long parentId) {
        return issueJpaRepository.sumChildrenStoryPoints(parentId);
    }

    @Override
    public IssueCountStats getChildIssueStats(Long parentId) {
        return issueJpaRepository.getChildIssueStats(parentId);
    }

    @Override
    public IssuePointStats getChildPointStats(Long parentId) {
        return issueJpaRepository.getChildPointStats(parentId);
    }

    @Override
    public List<Issue> findIncompleteIssuesBySprint(Sprint sprint, StateCategory doneCategory) {
        return issueJpaRepository.findIncompleteIssuesBySprint(sprint, doneCategory);
    }

    @Override
    public List<String> findIncompleteIssueKeysBySprint(Sprint sprint, StateCategory doneCategory) {
        return issueJpaRepository.findIncompleteIssueKeysBySprint(sprint, doneCategory);
    }

    @Override
    public List<String> findIssueKeysBySprint(Sprint sprint) {
        return issueJpaRepository.findIssueKeysBySprint(sprint);
    }

    @Override
    public List<Long> findStateIdsUsedByActiveIssues(Collection<Long> stateIds) {
        return issueJpaRepository.findStateIdsUsedByActiveIssues(stateIds);
    }

    @Override
    public List<IssueCountProjection> findActiveIssueCounts(Collection<Long> stateIds) {
        return issueJpaRepository.findActiveIssueCounts(stateIds);
    }

    @Override
    public boolean isAuthorOrAssignee(String workspaceKey, String issueKey, Long memberId) {
        return issueJpaRepository.isAuthorOrAssignee(workspaceKey, issueKey, memberId);
    }

    @Override
    public boolean isAuthor(String workspaceKey, String issueKey, Long memberId) {
        return issueJpaRepository.isAuthor(workspaceKey, issueKey, memberId);
    }

    @Override
    public Optional<WorkspaceMemberContact> findAuthorContact(String workspaceKey,
        String issueKey) {
        return issueJpaRepository.findAuthorContact(workspaceKey, issueKey);
    }

    @Override
    public Optional<WorkspaceMemberContact> findAssigneeContact(String workspaceKey,
        String issueKey) {
        return issueJpaRepository.findAssigneeContact(workspaceKey, issueKey);
    }

    @Override
    public Optional<WorkspaceMemberContact> findReporterContact(String workspaceKey,
        String issueKey) {
        return issueJpaRepository.findReporterContact(workspaceKey, issueKey);
    }

    @Override
    public List<WorkspaceMemberContact> findReviewerContacts(String workspaceKey, String issueKey) {
        return issueJpaRepository.findReviewerContacts(workspaceKey, issueKey);
    }

    @Override
    public List<WorkspaceMemberContact> findSubscriberContacts(String workspaceKey,
        String issueKey) {
        return issueJpaRepository.findSubscriberContacts(workspaceKey, issueKey);
    }

    // TODO: Consider using a single query if possible
    @Override
    public Set<WorkspaceMemberContact> findParticipantsContacts(String workspaceKey,
        String issueKey) {
        Set<WorkspaceMemberContact> targets = new HashSet<>();
        issueJpaRepository.findAuthorContact(workspaceKey, issueKey).ifPresent(targets::add);
        issueJpaRepository.findAssigneeContact(workspaceKey, issueKey).ifPresent(targets::add);
        issueJpaRepository.findReporterContact(workspaceKey, issueKey).ifPresent(targets::add);
        targets.addAll(issueJpaRepository.findSubscriberContacts(workspaceKey, issueKey));
        return targets;
    }

    @Override
    public Set<WorkspaceMemberContact> findParticipantsAndReviewersContacts(String workspaceKey,
        String issueKey) {
        Set<WorkspaceMemberContact> targets = findParticipantsContacts(workspaceKey, issueKey);
        targets.addAll(issueJpaRepository.findReviewerContacts(workspaceKey, issueKey));
        return targets;
    }
}
