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

    public void updateEnum(@Nullable EnumFieldOption value) {
        clearAndMarkPresent();
        this.enumOption = value;
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
        this.enumOption = null;
    }
}
