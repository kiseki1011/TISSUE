package com.tissue.issuetype.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.global.vo.Name;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectArchivedException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Getter
public class EnumFieldOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_field_id", nullable = false)
    private IssueField issueField;

    @Embedded
    private Name name;

    // TODO: Should i change it to "order"?
    @Column(nullable = false)
    private int position;

    @SuppressWarnings("NullAway.Init")
    protected EnumFieldOption() {}

    public static EnumFieldOption create(IssueField issueField, Name name, Integer position) {
        EnumFieldOption option = new EnumFieldOption();
        option.issueField = issueField;
        option.ensureEditable();
        option.name = name;
        option.position = (position == null) ? 0 : position;

        return option;
    }

    public String getName() {
        return name.toString();
    }

    public void rename(Name name) {
        ensureEditable();
        this.name = name;
    }

    public void movePositionTo(int position) {
        ensureEditable();
        this.position = position;
    }

    public void ensureEditable() {
        Project project = issueField.getIssueType().getProject();
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
