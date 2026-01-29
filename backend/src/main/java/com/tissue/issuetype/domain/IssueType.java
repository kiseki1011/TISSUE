package com.tissue.issuetype.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.project.domain.Project;
import com.tissue.workflow.domain.Workflow;
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
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@SQLRestriction("soft_deleted = false")
public class IssueType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Embedded
    private Name name;

    @Nullable
    @Column(name = "description", length = 255)
    private String description;

    // TODO: Add icon
    // private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueHierarchy issueHierarchy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    // TODO: change field name to "systemProvided"
    @Column(nullable = false)
    private boolean systemType;

    // TODO: should i make this(IssueType) bi-directional relation with IssueField?

    @SuppressWarnings("NullAway.Init")
    protected IssueType() {}

    public static IssueType create(
            Project project,
            Name name,
            @Nullable String description,
            ColorType color,
            IssueHierarchy issueHierarchy,
            Workflow workflow) {

        IssueType issueType = new IssueType();
        issueType.project = project;
        issueType.projectKey = project.getKey();
        issueType.workspaceKey = project.getWorkspaceKey();
        issueType.name = name;
        issueType.description = description;
        issueType.color = color;
        issueType.issueHierarchy = issueHierarchy;
        issueType.workflow = workflow;
        issueType.systemType = false;

        return issueType;
    }

    public boolean canUseStoryPoint() {
        return issueHierarchy.canUseStoryPoint();
    }

    public String getWorkspaceKey() {
        return project.getKey();
    }

    public String getDisplayName() {
        return name.getDisplay();
    }

    public void rename(Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }

    public void updateColor(ColorType color) {
        this.color = color;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public void setAsSystemType() {
        this.systemType = true;
    }
}
