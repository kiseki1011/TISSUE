package com.tissue.feature.issuetype.domain;

import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.HardDeleteEntity;
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
public class IssueField extends HardDeleteEntity {

    @Version
    private Long version;

    @Embedded
    private Name name;

    @Column(name = "description")
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueFieldType issueFieldType;

    @Column(name = "required", nullable = false)
    private boolean required;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_type_id", nullable = false)
    private IssueType issueType;

    // TODO: Add icon
    // private String icon;

    @SuppressWarnings("NullAway.Init")
    protected IssueField() {}

    public static IssueField create(
            Name name,
            @Nullable String description,
            IssueFieldType issueFieldType,
            boolean required,
            IssueType issueType) {
        IssueField issueField = new IssueField();
        issueField.name = name;
        issueField.description = Objects.requireNonNullElse(description, "");
        issueField.issueFieldType = issueFieldType;
        issueField.required = required;
        issueField.issueType = issueType;
        issueField.ensureEditable();

        return issueField;
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

    public void setRequired(boolean required) {
        ensureEditable();
        this.required = required;
    }

    public void ensureEditable() {
        Project project = issueType.getProject();
        if (issueType.getProject().isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
