package com.tissue.api.issue.domain.newmodel;

import java.time.LocalDate;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.model.Issue;

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
public class IssueFieldValue extends BaseEntity { // TODO: Do I need auditing for this entity too?

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// TODO: If Issue is deleted all the related values should be deleted too.
	// TODO: But Im considering using soft delete for Issue, or just manage Issue by status(step)

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Issue issue;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private IssueFieldDefinition field;

	private String stringValue;
	private Integer numberValue;
	private LocalDate dateValue;

	public static IssueFieldValue of(
		Issue issue,
		IssueFieldDefinition field,
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
}
