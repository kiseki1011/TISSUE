package com.tissue.feature.issuetype.domain;

import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.FIELD_TYPE_CANNOT_HAVE_OPTION;

import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
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
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "issue_field",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_issue_field_issue_type_id_normalized_name",
                    columnNames = {"issue_type_id", "normalized_name"})
        })
public class IssueField extends HardDeleteEntity {

    @Version
    private Long version;

    @Embedded
    private Name name;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueFieldType issueFieldType;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "position", nullable = false)
    private int position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_type_id", nullable = false)
    private IssueType issueType;

    @OrderBy("id ASC")
    @OneToMany(mappedBy = "issueField", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FieldOption> options = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected IssueField() {}

    static IssueField create(
            Name name,
            @Nullable String description,
            IssueFieldType issueFieldType,
            boolean required,
            IssueType issueType,
            int position) {
        IssueField issueField = new IssueField();
        issueField.name = name;
        issueField.description = Objects.requireNonNullElse(description, "");
        issueField.issueFieldType = issueFieldType;
        issueField.required = required;
        issueField.issueType = issueType;
        issueField.position = position;
        issueField.ensureEditable();

        return issueField;
    }

    public List<FieldOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public void addOption(Name optionName) {
        ensureEditable();
        ensureCanHaveOptions();
        FieldOption option = FieldOption.create(this, optionName);
        this.options.add(option);
    }

    private void ensureCanHaveOptions() {
        if (issueFieldType.canHaveOptions()) {
            return;
        }
        throw new BadRequestException(FIELD_TYPE_CANNOT_HAVE_OPTION);
    }

    public void removeOption(FieldOption option) {
        ensureEditable();
        this.options.remove(option);
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

    public void updatePosition(int position) {
        ensureEditable();
        this.position = position;
    }

    public void ensureEditable() {
        Project project = issueType.getProject();
        if (issueType.getProject().isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
