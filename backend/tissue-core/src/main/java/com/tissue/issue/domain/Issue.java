package com.tissue.issue.domain;

import static com.tissue.workflow.domain.enums.StateCategory.COMPLETED;
import static com.tissue.workflow.domain.enums.StateCategory.INITIAL;

import com.tissue.global.entity.SoftDeleteEntity;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issue.domain.exception.InvalidParentHierarchyException;
import com.tissue.issue.domain.exception.IssueSelfReferenceException;
import com.tissue.issue.domain.exception.OnlyInitialStateDeletionAllowedException;
import com.tissue.issue.domain.exception.ParentProjectMismatchException;
import com.tissue.issue.domain.exception.ParentRequiredException;
import com.tissue.issue.domain.exception.ParentWorkspaceMismatchException;
import com.tissue.issue.domain.exception.StoryPointNotAllowedException;
import com.tissue.issue.domain.vo.IssueKey;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.exception.ProjectArchivedException;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.WorkflowState;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@SQLRestriction("soft_deleted = false")
public class Issue extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "issue_key", nullable = false))
    private IssueKey key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Project project;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(name = "title", nullable = false)
    private String title;

    @Embedded
    private IssueContent content;

    @Embedded
    private IssueSchedule schedule;

    @Embedded
    private IssueProgress progress;

    @Embedded
    private IssueParticipants participants;

    @Embedded
    private IssueRelations relations;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private IssuePriority priority;

    @Nullable
    @Column(name = "story_point")
    private Integer storyPoint;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_issue_id")
    private Issue parentIssue;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_type_id")
    private IssueType issueType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_state_id")
    private WorkflowState currentState;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueFieldValue> fieldValues = new ArrayList<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueBranch> branches = new HashSet<>();

    // TODO: need to add Tag entity(used for search and categorization)

    @SuppressWarnings("NullAway.Init")
    protected Issue() {}

    public static Issue create(
            Project project,
            @Nullable Sprint sprint,
            IssueType issueType,
            String title,
            IssueContent content,
            IssueSchedule schedule,
            IssueParticipants participants,
            IssuePriority priority,
            @Nullable Integer storyPoint,
            @Nullable Issue parentIssue) {
        Issue issue = new Issue();
        issue.project = project;
        issue.ensureEditable();

        issue.workspaceKey = project.getWorkspaceKey();
        issue.sprint = sprint;
        issue.key = IssueKey.of(project.getKey(), project.generateNextIssueNumber());
        issue.issueType = issueType;
        issue.title = title;
        issue.content = content;
        issue.schedule = schedule;
        issue.participants = participants;
        issue.priority = priority;

        if (storyPoint != null) {
            issue.updateStoryPoint(storyPoint);
        }

        issue.progress = IssueProgress.init();
        issue.relations = IssueRelations.init();

        if (parentIssue != null) {
            issue.setParentIssue(parentIssue);
        }

        return issue;
    }

    public String getProjectKey() {
        return key.getProjectKey();
    }

    public String getKey() {
        return key.getValue();
    }

    public String getContent() {
        return content.getContent();
    }

    public String getSummary() {
        return content.getSummary();
    }

    public List<IssueFieldValue> getFieldValues() {
        return Collections.unmodifiableList(fieldValues);
    }

    public @Nullable String getParentKey() {
        return (this.parentIssue != null) ? this.parentIssue.getKey() : null;
    }

    public boolean isAuthor(Long memberId) {
        return getCreatedBy().equals(memberId);
    }

    public boolean isAssignee(Long projectMemberId) {
        return participants.isAssignee(projectMemberId);
    }

    public IssueFieldValue addOrUpdateFieldValue(IssueField field) {
        ensureEditable();
        return this.fieldValues.stream()
                .filter(fv -> fv.getField().equals(field))
                .findFirst()
                .orElseGet(() -> {
                    IssueFieldValue newValue = IssueFieldValue.of(this, field);
                    this.fieldValues.add(newValue);
                    return newValue;
                });
    }

    public void addBranch(IssueBranch branch) {
        ensureEditable();
        this.branches.add(branch);
    }

    public void setSprint(Sprint sprint) {
        ensureEditable();
        this.sprint = sprint;
    }

    public void clearSprint() {
        ensureEditable();
        this.sprint = null;
    }

    public void updateTitle(String title) {
        ensureEditable();
        this.title = title;
    }

    public void updateContent(@Nullable String content) {
        ensureEditable();
        this.content.updateContent(content);
    }

    public void updateSummary(@Nullable String summary) {
        ensureEditable();
        content.updateSummary(summary);
    }

    public void updateDueAt(@Nullable Instant dueAt) {
        ensureEditable();
        schedule.updateDueDate(dueAt);
    }

    public void updatePriority(IssuePriority priority) {
        ensureEditable();
        this.priority = priority;
    }

    public void updateStoryPoint(@Nullable Integer storyPoint) {
        ensureEditable();
        ensureCanModifyStoryPoint();
        this.storyPoint = storyPoint;
    }

    public void recalculateEpicStoryPoint(int totalChildrenStoryPoints) {
        if (getHierarchy().isNotEpic()) {
            return;
        }
        this.storyPoint = totalChildrenStoryPoints;
    }

    public void updateProgress(@Nullable Integer countBased, @Nullable Integer pointBased) {
        progress.update(countBased, pointBased);
    }

    public IssueRelation addRelation(Issue targetIssue, IssueRelationType type) {
        ensureEditable();
        targetIssue.ensureEditable();
        return relations.addRelation(this, targetIssue, type);
    }

    public IssueRelation removeRelation(Issue otherIssue) {
        ensureEditable();
        return relations.removeRelation(this, otherIssue);
    }

    public void transitionTo(WorkflowState newState) {
        ensureEditable();
        WorkflowState previousState = this.currentState;
        this.currentState = newState;

        if (previousState.isCategorizedAs(INITIAL)) {
            this.schedule.markStarted();
        }
        if (newState.isCategorizedAs(COMPLETED)) {
            this.schedule.markResolved();
        }
        if (previousState.isCategorizedAs(COMPLETED) && !newState.isCategorizedAs(COMPLETED)) {
            this.schedule.clearResolved();
        }
    }

    public void addSubscriber(ProjectMember projectMember) {
        ensureEditable();
        participants.addSubscriber(projectMember, this);
    }

    public void removeSubscriber(ProjectMember projectMember) {
        ensureEditable();
        participants.removeSubscriber(projectMember);
    }

    public void assignTo(ProjectMember assignee) {
        ensureEditable();
        participants.assignTo(assignee);
    }

    public void unassign() {
        ensureEditable();
        participants.unassign();
    }

    public void addReviewer(ProjectMember projectMember) {
        ensureEditable();
        participants.addReviewer(projectMember, this);
    }

    public void removeReviewer(ProjectMember projectMember) {
        ensureEditable();
        participants.removeReviewer(projectMember);
    }

    public int resetReviews(Set<Long> reviewerMemberIds) {
        return participants.resetReviews(reviewerMemberIds);
    }

    public void setParentIssue(Issue newParent) {
        ensureEditable();
        ensureCanSetParent(newParent);
        this.parentIssue = newParent;
    }

    public void removeParentIssue() {
        ensureEditable();
        ensureCanRemoveParent();
        parentIssue = null;
    }

    public void delete() {
        ensureEditable();
        ensureIsInitial();
        softDelete();
    }

    public IssueHierarchy getHierarchy() {
        return issueType.getIssueHierarchy();
    }

    public int getSubscribersCount() {
        return participants.getSubscribers().size();
    }

    private void ensureIsInitial() {
        if (!currentState.isCategorizedAs(INITIAL)) {
            throw new OnlyInitialStateDeletionAllowedException(
                    this.getWorkspaceKey(),
                    this.getKey(),
                    this.getCurrentState().getDisplayName(),
                    this.getCurrentState().getCategory());
        }
    }

    private void ensureCanModifyStoryPoint() {
        if (this.getHierarchy().cannotModifyStoryPoint()) {
            throw new StoryPointNotAllowedException(this.getWorkspaceKey(), this.getKey(), this.getHierarchy());
        }
    }

    private void ensureCanSetParent(Issue parentIssue) {
        ensureSameWorkspace(parentIssue);
        if (this.getHierarchy().cannotHaveCrossProjectParent()) {
            ensureSameProject(parentIssue);
        }
        ensureNotSelfReference(parentIssue);
        ensureValidParentHierarchy(parentIssue);
    }

    private void ensureValidParentHierarchy(Issue parentIssue) {
        IssueHierarchy parentHierarchy = parentIssue.getHierarchy();
        IssueHierarchy childHierarchy = this.getHierarchy();

        if (parentHierarchy.cannotBeParentOf(childHierarchy)) {
            throw new InvalidParentHierarchyException(
                    this.getWorkspaceKey(), parentIssue.getKey(), parentHierarchy, this.getKey(), childHierarchy);
        }
    }

    private void ensureNotSelfReference(Issue parentIssue) {
        if (this.equals(parentIssue)) {
            throw new IssueSelfReferenceException(this.getWorkspaceKey(), this.getKey());
        }
    }

    private void ensureSameWorkspace(Issue parentIssue) {
        boolean isDifferentWorkspace = !this.getWorkspaceKey().equals(parentIssue.getWorkspaceKey());
        if (isDifferentWorkspace) {
            throw new ParentWorkspaceMismatchException(
                    parentIssue.getWorkspaceKey(), parentIssue.getKey(), this.getWorkspaceKey(), this.getKey());
        }
    }

    private void ensureSameProject(Issue parentIssue) {
        boolean isDifferentProject = !this.getProjectKey().equals(parentIssue.getProjectKey());
        if (isDifferentProject) {
            throw new ParentProjectMismatchException(
                    parentIssue.getHierarchy(), parentIssue.getKey(), this.getHierarchy(), this.getKey());
        }
    }

    private void ensureCanRemoveParent() {
        if (getHierarchy().mustHaveParent()) {
            throw new ParentRequiredException(this.getWorkspaceKey(), this.getKey(), this.getHierarchy());
        }
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }

    @Override
    public String toString() {
        return "Issue{id=%d, key='%s', project='%s', workspace='%s', title='%s'}"
                .formatted(id, key, getProjectKey(), workspaceKey, title);
    }
}
