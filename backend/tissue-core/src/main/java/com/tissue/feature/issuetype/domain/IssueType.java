package com.tissue.feature.issuetype.domain;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "issue_type",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_issue_type_project_id_normalized",
                    columnNames = {"project_id", "normalized_name"})
        })
public class IssueType extends HardDeleteEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

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

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "issueType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueField> fields = new ArrayList<>();

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
        issueType.name = name;
        issueType.description = Objects.requireNonNullElse(description, "");
        issueType.color = color;
        issueType.icon = icon;
        issueType.issueHierarchy = issueHierarchy;
        issueType.workflow = workflow;
        issueType.systemProvided = false;

        return issueType;
    }

    public IssueField addField(
            Name fieldName, @Nullable String description, IssueFieldType type, boolean required, int position) {

        ensureEditable();
        IssueField field = IssueField.create(fieldName, description, type, required, this, position);
        this.fields.add(field);
        return field;
    }

    public List<IssueField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void reorderFields(List<Long> orderedIds) {
        ensureEditable();

        Map<Long, IssueField> fieldMap = this.fields.stream().collect(Collectors.toMap(IssueField::getId, f -> f));

        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            IssueField field = fieldMap.get(id);
            if (field != null) {
                field.updatePosition(i);
            }
        }
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
