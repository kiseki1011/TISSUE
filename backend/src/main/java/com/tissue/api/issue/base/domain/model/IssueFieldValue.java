package com.tissue.api.issue.base.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@SQLRestriction("archived = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueFieldValue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	private Instant dateValue;

	public static IssueFieldValue of(Issue issue, IssueField field, Object value) {
		IssueFieldValue issueFieldValue = new IssueFieldValue();
		issueFieldValue.issue = issue;
		issueFieldValue.field = field;
		issueFieldValue.apply(value);
		return issueFieldValue;
	}

	public void updateValue(Object value) {
		if (field.isRequired() && value == null) {
			throw new InvalidOperationException("This field is required.");
		}
		if (value == null) {
			clearValue();
			return;
		}
		apply(value);
	}

	private void apply(Object value) {
		clearValue();
		switch (field.getFieldType()) {
			case TEXT -> this.stringValue = (String)value;
			case INTEGER -> this.integerValue = (Integer)value;
			case DECIMAL -> this.decimalValue = (BigDecimal)value;
			case DATE -> this.dateValue = (Instant)value;
			case ENUM -> {
				EnumFieldOption opt = (EnumFieldOption)value;
				this.enumOption = opt;
				this.stringValue = opt.getLabel();
			}
			default -> throw new InvalidCustomFieldException("Unsupported: " + field.getFieldType());
		}
	}

	public void clearValue() {
		this.stringValue = null;
		this.integerValue = null;
		this.decimalValue = null;
		this.dateValue = null;
		this.enumOption = null;
	}
}
