package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ISSUE_IN_PROGRESS_DELETION_NOT_ALLOWED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ISSUE_SELF_REFERENCE;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.PARENT_PROJECT_MISMATCH;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.PARENT_REQUIRED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.STORY_POINT_NOT_ALLOWED;
import static com.tissue.feature.workflow.domain.enums.StateCategory.INITIAL;
import static com.tissue.shared.exception.ErrorContextKeys.HIERARCHIES_REQUIRING_PARENT;
import static com.tissue.shared.exception.ErrorContextKeys.STORY_POINT_ALLOWED_HIERARCHIES;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.exception.InvalidParentHierarchyException;
import com.tissue.feature.issue.domain.vo.IssueKey;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "issue",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_issue_project_id_issue_key",
                    columnNames = {"project_id", "issue_key"})
        },
        indexes = {
            @Index(name = "idx_issue_project_priority_due", columnList = "project_id, priority, due_at"),
            @Index(name = "idx_issue_current_state_id", columnList = "current_state_id")
        })
@SQLRestriction("soft_deleted = false")
public class Issue extends SoftDeleteEntity {

    @Version
    private Long version;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "issue_key", nullable = false))
    private IssueKey key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private Map<String, Object> customFields = new HashMap<>();

    /**
     * Generated tsvector column produced from issue_key + title + content.
     * Owned by PostgreSQL (see {@code tissue-bootstrap/src/main/resources/db/fts.sql} for the DDL);
     * the mapping exists only so Specifications can reference it via
     * {@code root.get("searchVector")} inside {@code fts_match()} calls.
     *
     * <p>byte[] type avoids Hibernate's tsvector→String conversion failure —
     * PostgreSQL returns tsvector as binary, and we never read the value
     * directly from Java code anyway.
     */
    @Nullable
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "search_vector", insertable = false, updatable = false, columnDefinition = "tsvector")
    private byte[] searchVector;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueBranch> branches = new HashSet<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueTag> tags = new HashSet<>();

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

        issue.sprint = sprint;
        issue.key = IssueKey.of(project.getKey(), project.generateNextIssueNumber());
        issue.issueType = issueType;
        issue.currentState = issueType.getWorkflow().getInitialState();
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

    public Map<String, Object> getCustomFields() {
        return Collections.unmodifiableMap(customFields);
    }

    public @Nullable String getParentKey() {
        return (this.parentIssue != null) ? this.parentIssue.getKey() : null;
    }

    public boolean isAuthor(Long memberId) {
        return getCreatedBy().equals(memberId);
    }

    public void setCustomFieldValue(String fieldIdStr, Object jsonValue) {
        ensureEditable();
        this.customFields.put(fieldIdStr, jsonValue);
    }

    public void clearCustomField(String fieldIdStr) {
        ensureEditable();
        this.customFields.remove(fieldIdStr);
    }

    public void addBranch(IssueBranch branch) {
        ensureEditable();
        this.branches.add(branch);
    }

    public void addTag(Tag tag) {
        ensureEditable();
        boolean alreadyTagged = tags.stream().anyMatch(it -> it.getTag().equals(tag));
        if (alreadyTagged) {
            return;
        }
        tags.add(new IssueTag(this, tag));
    }

    public void removeTag(Tag tag) {
        ensureEditable();
        tags.removeIf(it -> it.getTag().equals(tag));
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
        this.content.updateSummary(summary);
    }

    public void updateDueAt(@Nullable Instant dueAt) {
        ensureEditable();
        this.schedule.updateDueDate(dueAt);
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
        return relations.removeRelation(otherIssue);
    }

    public void transitionTo(WorkflowState newState) {
        ensureEditable();
        WorkflowState previousState = this.currentState;
        this.currentState = newState;

        if (previousState.isCategorizedAs(INITIAL)) {
            this.schedule.markStarted();
        }
        if (newState.getCategory().isTerminal()) {
            this.schedule.markResolved();
        }
        if (previousState.getCategory().isTerminal() && !newState.getCategory().isTerminal()) {
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

    public void claimBy(ProjectMember claimer) {
        ensureEditable();
        participants.claimBy(claimer);
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
            throw new BadRequestException(ISSUE_IN_PROGRESS_DELETION_NOT_ALLOWED);
        }
    }

    private void ensureCanModifyStoryPoint() {
        if (this.getHierarchy().cannotModifyStoryPoint()) {
            throw new BadRequestException(STORY_POINT_NOT_ALLOWED)
                    .addContext(STORY_POINT_ALLOWED_HIERARCHIES, IssueHierarchy.getStoryPointModifiable());
        }
    }

    private void ensureCanSetParent(Issue parentIssue) {
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
                    parentIssue.getKey(), parentHierarchy, this.getKey(), childHierarchy);
        }
    }

    private void ensureNotSelfReference(Issue parentIssue) {
        if (this.equals(parentIssue)) {
            throw new BadRequestException(ISSUE_SELF_REFERENCE);
        }
    }

    private void ensureSameProject(Issue parentIssue) {
        boolean isDifferentProject = !Objects.equals(this.getProjectKey(), parentIssue.getProjectKey());
        if (isDifferentProject) {
            throw new BadRequestException(PARENT_PROJECT_MISMATCH);
        }
    }

    private void ensureCanRemoveParent() {
        if (getHierarchy().mustHaveParent()) {
            throw new BadRequestException(PARENT_REQUIRED)
                    .addContext(HIERARCHIES_REQUIRING_PARENT, IssueHierarchy.getParentRequired());
        }
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getKey());
        }
    }
}
