package com.tissue.api.issue.base.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidCustomFieldException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

// TODO: archived=false 대상으로 issue_id, issue_field_id 대상 유니크 제약 추후에 설정(Postgres DDL)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueFieldValue extends BaseEntity {

	@Version
	private Long version;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "field_value_seq_gen")
	@SequenceGenerator(name = "field_value_seq_gen", sequenceName = "field_value_seq", allocationSize = 50)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Issue issue;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private IssueField field;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "enum_option_id")
	private EnumFieldOption enumOption;

	private String stringValue;
	private Integer integerValue;
	private BigDecimal decimalValue;
	private Instant timestampValue;
	private LocalDate dateValue;
	private Boolean booleanValue;

	@Column(name = "value_present", nullable = false)
	private boolean valuePresent;

	public static IssueFieldValue of(@NonNull Issue issue, @NonNull IssueField field) {
		IssueFieldValue fieldValue = new IssueFieldValue();
		fieldValue.issue = issue;
		fieldValue.field = field;
		fieldValue.valuePresent = false;
		return fieldValue;
	}

	public void apply(@NonNull Object value) {
		clearColumnsOnly();
		switch (field.getFieldType()) {
			case TEXT -> this.stringValue = (String)value;
			case INTEGER -> this.integerValue = (Integer)value;
			case DECIMAL -> this.decimalValue = (BigDecimal)value;
			case TIMESTAMP -> this.timestampValue = (Instant)value;
			case DATE -> this.dateValue = (LocalDate)value;
			case BOOLEAN -> this.booleanValue = (Boolean)value;
			case ENUM -> this.enumOption = (EnumFieldOption)value;
			default -> throw new InvalidCustomFieldException("Unsupported: " + field.getFieldType());
		}
		markPresent();
	}

	public void clearValue() {
		clearColumnsOnly();
		markEmpty();
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
