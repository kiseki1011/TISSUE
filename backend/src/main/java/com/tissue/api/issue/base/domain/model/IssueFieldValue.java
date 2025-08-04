package com.tissue.api.issue.base.domain.model;

import java.time.LocalDate;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidCustomFieldException;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueFieldValue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Issue issue;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private IssueField field;

	private String stringValue;
	private Integer numberValue;
	private LocalDate dateValue;

	public static IssueFieldValue of(
		Issue issue,
		IssueField field,
		Object value
	) {
		IssueFieldValue val = new IssueFieldValue();
		val.issue = issue;
		val.field = field;

		switch (field.getFieldType()) {
			case TEXT, ENUM -> val.stringValue = value.toString();
			case NUMBER -> val.numberValue = ((Number)value).intValue();
			case DATE -> val.dateValue = LocalDate.parse(value.toString());
		}
		return val;
	}

	public void updateValue(Object value) {
		if (value == null) {
			return;
		}

		switch (field.getFieldType()) {
			case TEXT, ENUM -> this.stringValue = value.toString();
			case NUMBER -> this.numberValue = ((Number)value).intValue();
			case DATE -> this.dateValue = LocalDate.parse(value.toString());
			default -> throw new InvalidCustomFieldException("Unsupported field type: " + field.getFieldType());
		}
	}
}
