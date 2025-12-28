package com.tissue.issue.domain;

import static com.tissue.workflow.domain.enums.StateCategory.COMPLETED;
import static com.tissue.workflow.domain.enums.StateCategory.INITIAL;

import com.tissue.common.entity.BaseEntity;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.issue.domain.vo.IssueKey;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

@Entity
@Getter
@SQLRestriction("softDeleted = false")
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Embedded private IssueContent content;

    @Embedded private IssueSchedule schedule;

    @Embedded private IssueProgress progress;

    @Embedded private IssueParticipants participants;

    @Embedded private IssueRelations relations;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private IssuePriority priority;

    @Column(name = "story_point")
    private Integer storyPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_issue_id")
    private Issue parentIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    private IssueType issueType;

    @ManyToOne(fetch = FetchType.LAZY)
    private WorkflowState currentState;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueFieldValue> fieldValues = new ArrayList<>();

    // TODO: need to add Tag entity(used for search and categorization)

    protected Issue() {}

    public static Issue create(
            @NonNull Project project,
            @Nullable Sprint sprint,
            @NonNull IssueType issueType,
            @NonNull String title,
            @NonNull IssueContent content,
            @NonNull IssueSchedule schedule,
            @NonNull IssueParticipants participants,
            @Nullable IssuePriority priority,
            @Nullable Integer storyPoint,
            @Nullable Issue parentIssue) {
        Issue issue = new Issue();
        issue.project = project;
        issue.workspaceKey = project.getWorkspaceKey();
        issue.sprint = sprint;
        issue.key = IssueKey.of(project.getKey(), project.generateNextIssueNumber());
        issue.issueType = issueType;
        issue.title = title;
        issue.content = content;
        issue.schedule = schedule;
        issue.participants = participants;
        issue.priority = priority == null ? IssuePriority.NORMAL : priority;

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

    public IssueFieldValue addOrUpdateFieldValue(IssueField field) {
        return this.fieldValues.stream()
                .filter(fv -> fv.getField().equals(field))
                .findFirst()
                .orElseGet(
                        () -> {
                            IssueFieldValue newValue = IssueFieldValue.of(this, field);
                            this.fieldValues.add(newValue);
                            return newValue;
                        });
    }

    public void setSprint(@NonNull Sprint sprint) {
        this.sprint = sprint;
    }

    public void clearSprint() {
        this.sprint = null;
    }

    public void changeReporter(@NonNull ProjectMember reporter) {
        if (participants.isReporter(reporter)) {
            return;
        }
        participants.changeReporter(reporter);
    }

    public void updateTitle(@NonNull String title) {
        this.title = title;
    }

    public void updateContent(@Nullable String content) {
        this.content.updateContent(content);
    }

    public void updateSummary(@Nullable String summary) {
        content.updateSummary(summary);
    }

    public void updateDueAt(@Nullable Instant dueAt) {
        schedule.updateDueDate(dueAt);
    }

    public void updatePriority(@NonNull IssuePriority priority) {
        this.priority = priority;
    }

    public void updateStoryPoint(@Nullable Integer storyPoint) {
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

    public IssueRelation addRelation(@NonNull Issue targetIssue, @NonNull IssueRelationType type) {
        return relations.addRelation(this, targetIssue, type);
    }

    public IssueRelation removeRelation(@NonNull Issue otherIssue) {
        return relations.removeRelation(otherIssue);
    }

    // TODO: should i separate this to a separate domain service?
    public void transitionTo(@NonNull WorkflowState newState) {
        WorkflowState previousState = this.currentState;
        this.currentState = newState;

        if (previousState.getCategory().isInitial()) {
            this.schedule.markStarted();
        }
        if (newState.getCategory().isCompleted()) {
            this.schedule.markResolved();
        }
        if (previousState.isCategorizedAs(COMPLETED) && !newState.isCategorizedAs(COMPLETED)) {
            this.schedule.clearResolved();
        }
    }

    public void addSubscriber(@NonNull ProjectMember projectMember) {
        participants.addSubscriber(projectMember, this);
    }

    public void removeSubscriber(@NonNull ProjectMember projectMember) {
        participants.removeSubscriber(projectMember);
    }

    public void assignTo(@NonNull ProjectMember assignee) {
        participants.assignTo(assignee);
    }

    public void unassign() {
        participants.unassign();
    }

    public void addReviewer(@NonNull ProjectMember projectMember) {
        participants.addReviewer(projectMember, this);
    }

    public void removeReviewer(@NonNull ProjectMember projectMember) {
        participants.removeReviewer(projectMember);
    }

    public void setParentIssue(@NonNull Issue newParent) {
        ensureCanSetParent(newParent);
        this.parentIssue = newParent;
    }

    public void removeParentIssue() {
        ensureCanRemoveParent();
        parentIssue = null;
    }

    public void delete() {
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
            throw IssueExceptions.onlyInitialStateDeletionAllowed(
                    this.getWorkspaceKey(),
                    this.getKey().toString(),
                    this.getCurrentState().getDisplayName(),
                    this.getCurrentState().getCategory());
        }
    }

    private void ensureCanModifyStoryPoint() {
        if (this.getHierarchy().cannotModifyStoryPoint()) {
            throw IssueExceptions.storyPointNotAllowed(
                    this.getWorkspaceKey(), this.getKey().toString(), this.getHierarchy());
        }
    }

    private void ensureCanSetParent(@NonNull Issue parentIssue) {
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
            throw IssueExceptions.invalidParentHierarchy(
                    this.getWorkspaceKey(),
                    parentIssue.getKey().toString(),
                    parentHierarchy,
                    this.getKey().toString(),
                    childHierarchy);
        }
    }

    private void ensureNotSelfReference(Issue parentIssue) {
        if (this.equals(parentIssue)) {
            throw IssueExceptions.issueSelfReference(
                    this.getWorkspaceKey(), this.getKey().toString());
        }
    }

    private void ensureSameWorkspace(Issue parentIssue) {
        boolean isDifferentWorkspace =
                !this.getWorkspaceKey().equals(parentIssue.getWorkspaceKey());
        if (isDifferentWorkspace) {
            throw IssueExceptions.parentWorkspaceMismatch(
                    parentIssue.getWorkspaceKey(),
                    parentIssue.getKey().toString(),
                    this.getWorkspaceKey(),
                    this.getKey().toString());
        }
    }

    private void ensureSameProject(Issue parentIssue) {
        boolean isDifferentProject = !this.getProjectKey().equals(parentIssue.getProjectKey());
        if (isDifferentProject) {
            throw IssueExceptions.parentProjectMismatch(
                    parentIssue.getHierarchy(),
                    parentIssue.getKey().toString(),
                    this.getHierarchy(),
                    this.getKey().toString());
        }
    }

    private void ensureCanRemoveParent() {
        if (getHierarchy().mustHaveParent()) {
            throw IssueExceptions.parentRequired(
                    this.getWorkspaceKey(), this.getKey().toString(), this.getHierarchy());
        }
    }

    @Override
    public String toString() {
        return "Issue{id=%d, key='%s', project='%s', workspace='%s', title='%s'}"
                .formatted(id, key, getProjectKey(), workspaceKey, title);
    }
}
