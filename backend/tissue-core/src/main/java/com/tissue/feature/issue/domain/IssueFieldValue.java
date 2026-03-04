package com.tissue.feature.issue.domain;

import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class IssueFieldValue extends HardDeleteEntity {

    @Version
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Issue issue;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(name = "issue_key", nullable = false, updatable = false)
    private String issueKey;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IssueField field;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_option_id")
    private FieldOption fieldOption;

    @Nullable
    private String stringValue;

    @Nullable
    private Integer integerValue;

    @Nullable
    private BigDecimal decimalValue;

    @Nullable
    private Instant timestampValue;

    @Nullable
    private LocalDate dateValue;

    @Nullable
    private Boolean booleanValue;

    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_value", columnDefinition = "jsonb")
    private Map<Long, Boolean> checklistMap;

    @Column(name = "value_present", nullable = false)
    private boolean valuePresent;

    @SuppressWarnings("NullAway.Init")
    protected IssueFieldValue() {}

    public static IssueFieldValue of(Issue issue, IssueField field) {
        IssueFieldValue fieldValue = new IssueFieldValue();
        fieldValue.issue = issue;
        fieldValue.workspaceKey = issue.getWorkspaceKey();
        fieldValue.issueKey = issue.getKey();
        fieldValue.field = field;
        fieldValue.valuePresent = false;
        return fieldValue;
    }

    public void updateText(@Nullable String value) {
        clearAndMarkPresent();
        this.stringValue = value;
    }

    public void updateInteger(@Nullable Integer value) {
        clearAndMarkPresent();
        this.integerValue = value;
    }

    public void updateDecimal(@Nullable BigDecimal value) {
        clearAndMarkPresent();
        this.decimalValue = value;
    }

    public void updateTimestamp(@Nullable Instant value) {
        clearAndMarkPresent();
        this.timestampValue = value;
    }

    public void updateDate(@Nullable LocalDate value) {
        clearAndMarkPresent();
        this.dateValue = value;
    }

    public void updateBoolean(@Nullable Boolean value) {
        clearAndMarkPresent();
        this.booleanValue = value;
    }

    public void updateSelectOption(@Nullable FieldOption value) {
        clearAndMarkPresent();
        this.fieldOption = value;
    }

    public void updateChecklistMap(@Nullable Map<Long, Boolean> booleanMap) {
        clearAndMarkPresent();
        this.checklistMap = booleanMap;
    }

    public void clearValue() {
        clearColumnsOnly();
        this.valuePresent = false;
    }

    private void clearAndMarkPresent() {
        clearColumnsOnly();
        this.valuePresent = true;
    }

    private void clearColumnsOnly() {
        this.stringValue = null;
        this.integerValue = null;
        this.decimalValue = null;
        this.timestampValue = null;
        this.dateValue = null;
        this.booleanValue = null;
        this.fieldOption = null;
        this.checklistMap = null;
    }
}
