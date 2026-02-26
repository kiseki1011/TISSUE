package com.tissue.feature.issuetype.domain;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class IssueType extends HardDeleteEntity {

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

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "icon", nullable = false)
    private IconType icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueHierarchy issueHierarchy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private boolean systemProvided;

    // TODO: should i make this(IssueType) bi-directional relation with IssueField?
    //  - im considering adding support to be able to set the order of the fields (for ui purpose)

    @SuppressWarnings("NullAway.Init")
    protected IssueType() {}

    public static IssueType create(
            Project project,
            Name name,
            @Nullable String description,
            ColorType color,
            IconType icon,
            IssueHierarchy issueHierarchy,
            Workflow workflow) {

        IssueType issueType = new IssueType();
        issueType.project = project;
        issueType.ensureEditable();
        issueType.projectKey = project.getKey();
        issueType.name = name;
        issueType.description = Objects.requireNonNullElse(description, "");
        issueType.color = color;
        issueType.icon = icon;
        issueType.issueHierarchy = issueHierarchy;
        issueType.workflow = workflow;
        issueType.systemProvided = false;

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
    }

    public void updateColor(ColorType color) {
        ensureEditable();
        this.color = color;
    }

    public void updateIcon(IconType icon) {
        ensureEditable();
        this.icon = icon;
    }

    public void setWorkflow(Workflow workflow) {
        ensureEditable();
        this.workflow = workflow;
    }

    public void setAsSystemProvided() {
        ensureEditable();
        this.systemProvided = true;
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
