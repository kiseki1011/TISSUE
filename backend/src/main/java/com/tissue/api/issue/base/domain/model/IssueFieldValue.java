package com.tissue.api.issue.base.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.common.exception.type.InvalidOperationException;

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
@SQLRestriction("archived = false")
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

	private IssueFieldValue(Issue issue, IssueField field) {
		this.issue = issue;
		this.field = field;
	}

	public static IssueFieldValue of(@NonNull Issue issue, @NonNull IssueField field) {
		return new IssueFieldValue(issue, field);
	}

	public void updateValue(Object value) {
		ensureValuePresentRequired(value, this.field);
		if (value == null) {
			clearValue();
			return;
		}
		apply(value);
	}

	public void apply(Object value) {
		clearValue();
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
	}

	public void clearValue() {
		this.stringValue = null;
		this.integerValue = null;
		this.decimalValue = null;
		this.timestampValue = null;
		this.dateValue = null;
		this.booleanValue = null;
		this.enumOption = null;
	}

	private static void ensureValuePresentRequired(Object value, IssueField field) {
		if (field.isRequired() && (value == null || isBlankString(value))) {
			throw new InvalidOperationException("This field is required.");
		}
	}

	private static boolean isBlankString(Object value) {
		return (value instanceof String s) && s.isBlank();
	}
}
