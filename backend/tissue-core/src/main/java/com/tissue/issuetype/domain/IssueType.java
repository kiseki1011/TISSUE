package com.tissue.issuetype.domain;

import com.tissue.enums.ColorType;
import com.tissue.global.entity.BaseEntity;
import com.tissue.global.vo.Name;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectArchivedException;
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
import java.util.Objects;
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

    // TODO: 굳이 편의를 위해 둬야할까? 어차피 대부분 Project를 Join fetch 할텐데
    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

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
        issueType.ensureEditable();
        issueType.projectKey = project.getKey();
        issueType.name = name;
        issueType.description = Objects.requireNonNullElse(description, "");
        issueType.color = color;
        issueType.issueHierarchy = issueHierarchy;
        issueType.workflow = workflow;
        issueType.systemType = false;

        return issueType;
    }

    public boolean canUseStoryPoint() {
        return issueHierarchy.canUseStoryPoint();
    }

    public String getName() {
        return name.toString();
    }

    public void rename(Name name) {
        ensureEditable();
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        ensureEditable();
        this.description = Objects.requireNonNullElse(description, "");
        ;
    }

    public void updateColor(ColorType color) {
        ensureEditable();
        this.color = color;
    }

    public void setWorkflow(Workflow workflow) {
        ensureEditable();
        this.workflow = workflow;
    }

    public void setAsSystemProvided() {
        ensureEditable();
        this.systemType = true;
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
