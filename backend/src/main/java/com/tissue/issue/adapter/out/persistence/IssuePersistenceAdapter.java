package com.tissue.issue.adapter.out.persistence;

import com.tissue.issue.adapter.out.persistence.repository.IssueJpaRepository;
import com.tissue.issue.application.dto.IssueCountProjection;
import com.tissue.issue.application.dto.IssueCountStats;
import com.tissue.issue.application.dto.IssuePointStats;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.enums.StateCategory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    public List<Issue> findByKeyInAndWorkspaceKey(Collection<String> issueKeys, String workspaceKey) {
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
}
