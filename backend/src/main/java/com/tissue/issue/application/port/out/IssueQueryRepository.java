package com.tissue.issue.application.port.out;

import com.tissue.issue.application.dto.IssueCountProjection;
import com.tissue.issue.application.dto.IssueCountStats;
import com.tissue.issue.application.dto.IssuePointStats;
import com.tissue.issue.domain.Issue;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueQueryRepository {

    Optional<Issue> findById(Long id);

    Optional<Issue> findByKeyAndProject(String issueKey, Project project);

    List<Issue> findByKeyInAndWorkspaceKey(Collection<String> issueKeys, String workspaceKey);

    Optional<Issue> findWithBasicInfo(String workspaceKey, String issueKey);

    Optional<Issue> findWithDetail(String workspaceKey, String issueKey);

    Optional<Issue> findWithParent(String workspaceKey, String issueKey);

    List<Issue> findChildren(String workspaceKey, String issueKey);

    boolean hasChildren(String workspaceKey, String issueKey);

    boolean existsByIssueType(IssueType issueType);

    Integer sumChildrenStoryPoints(Long parentId);

    IssueCountStats getChildIssueStats(Long parentId);

    IssuePointStats getChildPointStats(Long parentId);

    List<Issue> findIncompleteIssuesBySprint(Sprint sprint, StateCategory doneCategory);

    List<String> findIncompleteIssueKeysBySprint(Sprint sprint, StateCategory doneCategory);

    List<String> findIssueKeysBySprint(Sprint sprint);

    List<Long> findStateIdsUsedByActiveIssues(Collection<Long> stateIds);

    List<IssueCountProjection> findActiveIssueCounts(Collection<Long> stateIds);

    boolean isAuthorOrAssignee(String workspaceKey, String issueKey, Long memberId);

    boolean isAuthor(String workspaceKey, String issueKey, Long memberId);

    // Notification Support
    Optional<WorkspaceMemberContact> findAuthorContact(String workspaceKey, String issueKey);

    Optional<WorkspaceMemberContact> findAssigneeContact(String workspaceKey, String issueKey);

    Optional<WorkspaceMemberContact> findReporterContact(String workspaceKey, String issueKey);

    List<WorkspaceMemberContact> findReviewerContacts(String workspaceKey, String issueKey);

    List<WorkspaceMemberContact> findSubscriberContacts(String workspaceKey, String issueKey);
}
