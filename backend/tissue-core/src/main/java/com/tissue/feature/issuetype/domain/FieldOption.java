package com.tissue.feature.issuetype.domain;

import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.vo.Name;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_field_option_issue_field_id_normalized_name",
                    columnNames = {"issue_field_id", "normalized_name"})
        },
        indexes = {@Index(name = "idx_field_option_issue_field_id", columnList = "issue_field_id")})
@Getter
public class FieldOption extends HardDeleteEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_field_id", nullable = false)
    private IssueField issueField;

    @Embedded
    private Name name;

    @SuppressWarnings("NullAway.Init")
    protected FieldOption() {}

    public static FieldOption create(IssueField issueField, Name name) {
        FieldOption option = new FieldOption();
        option.issueField = issueField;
        option.name = name;

        return option;
    }

    public String getName() {
        return name.toString();
    }

    public void rename(Name name) {
        this.name = name;
    }
}
