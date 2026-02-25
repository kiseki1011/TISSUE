package com.tissue.feature.issuetype.domain;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.vo.Name;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Getter
public class EnumFieldOption extends HardDeleteEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_field_id", nullable = false)
    private IssueField issueField;

    @Embedded
    private Name name;

    @SuppressWarnings("NullAway.Init")
    protected EnumFieldOption() {}

    public static EnumFieldOption create(IssueField issueField, Name name) {
        EnumFieldOption option = new EnumFieldOption();
        option.issueField = issueField;
        option.ensureEditable();
        option.name = name;

        return option;
    }

    public String getName() {
        return name.toString();
    }

    public void rename(Name name) {
        ensureEditable();
        this.name = name;
    }

    public void ensureEditable() {
        Project project = issueField.getIssueType().getProject();
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
