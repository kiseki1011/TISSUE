package com.tissue.feature.issue.domain;

import com.tissue.feature.issuetype.domain.EnumFieldOption;
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
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class IssueFieldValue extends HardDeleteEntity {

    @Version
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Issue issue;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IssueField field;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enum_option_id")
    private EnumFieldOption enumOption;

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

    @Column(name = "value_present", nullable = false)
    private boolean valuePresent;

    @SuppressWarnings("NullAway.Init")
    protected IssueFieldValue() {}

    public static IssueFieldValue of(Issue issue, IssueField field) {
        IssueFieldValue fieldValue = new IssueFieldValue();
        fieldValue.issue = issue;
        fieldValue.field = field;
        fieldValue.valuePresent = false;
        return fieldValue;
    }

    public void apply(@Nullable Object value) {
        clearColumnsOnly();
        switch (field.getIssueFieldType()) {
            case TEXT -> this.stringValue = (String) value;
            case INTEGER -> this.integerValue = (Integer) value;
            case DECIMAL -> this.decimalValue = (BigDecimal) value;
            case TIMESTAMP -> this.timestampValue = (Instant) value;
            case DATE -> this.dateValue = (LocalDate) value;
            case BOOLEAN -> this.booleanValue = (Boolean) value;
            case ENUM -> this.enumOption = (EnumFieldOption) value;
            default -> throw new IllegalArgumentException("Unsupported field type: " + field.getIssueFieldType());
        }
        markPresent();
    }

    public void clearValue() {
        clearColumnsOnly();
        markEmpty();
    }

    public @Nullable Object getValue() {
        if (!this.valuePresent) {
            return null;
        }

        return switch (field.getIssueFieldType()) {
            case TEXT -> this.stringValue;
            case INTEGER -> this.integerValue;
            case DECIMAL -> this.decimalValue;
            case TIMESTAMP -> this.timestampValue;
            case DATE -> this.dateValue;
            case BOOLEAN -> this.booleanValue;
            case ENUM -> this.enumOption;
            default -> throw new IllegalArgumentException("Unexpected field type: " + field.getIssueFieldType());
        };
    }

    private void clearColumnsOnly() {
        this.stringValue = null;
        this.integerValue = null;
        this.decimalValue = null;
        this.timestampValue = null;
        this.dateValue = null;
        this.booleanValue = null;
        this.enumOption = null;
    }

    private void markPresent() {
        this.valuePresent = true;
    }

    private void markEmpty() {
        this.valuePresent = false;
    }
}
