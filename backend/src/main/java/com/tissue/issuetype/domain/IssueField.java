package com.tissue.issuetype.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.domain.enums.IssueFieldType;
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
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class IssueField extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Embedded
    private Name name;

    @Nullable
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueFieldType issueFieldType;

    @Column(name = "required", nullable = false)
    private boolean required;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_type_id", nullable = false)
    private IssueType issueType;

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
        issueField.description = description;
        issueField.issueFieldType = issueFieldType;
        issueField.required = required;
        issueField.issueType = issueType;

        return issueField;
    }

    public String getWorkspaceKey() {
        return issueType.getWorkspaceKey();
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

    public void setRequired(boolean required) {
        this.required = required;
    }
}
