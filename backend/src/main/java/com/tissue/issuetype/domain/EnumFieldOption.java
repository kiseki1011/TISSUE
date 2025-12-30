package com.tissue.issuetype.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.vo.Name;
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

    // TODO: should i change it to "order"?
    @Column(nullable = false)
    private int position;

    @SuppressWarnings("NullAway.Init")
    protected EnumFieldOption() {}

    public static EnumFieldOption create(IssueField issueField, Name name, Integer position) {
        EnumFieldOption option = new EnumFieldOption();
        option.issueField = issueField;
        option.name = name;
        option.position = (position == null) ? 0 : position;

        return option;
    }

    public String getDisplayName() {
        return name.getDisplay();
    }

    public void rename(Name name) {
        this.name = name;
    }

    public void movePositionTo(int position) {
        this.position = position;
    }
}
