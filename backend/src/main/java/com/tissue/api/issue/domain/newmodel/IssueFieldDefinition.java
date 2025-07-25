package com.tissue.api.issue.domain.newmodel;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.model.enums.FieldType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"issueType_id", "label"})
})
@EqualsAndHashCode(of = {"issueType", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueFieldDefinition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String key;

	@Column(nullable = false)
	private String label; // UI label

	@Column(nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FieldType fieldType; // TEXT, NUMBER, DATE, ENUM, etc.

	@Column(nullable = false)
	private boolean required;

	// TODO: Should I use a bi-directional relation with IssueTypeDefinition?
	// TODO: Is @JoinColumn needed or recommended?
	@ManyToOne(fetch = FetchType.LAZY)
	private IssueTypeDefinition issueType;

	@Builder
	public IssueFieldDefinition(
		String key,
		String label,
		String description,
		FieldType fieldType,
		Boolean required,
		IssueTypeDefinition issueType
	) {
		this.key = key;
		this.label = label;
		this.description = (description != null) ? description : "";
		this.fieldType = fieldType;
		this.required = (required != null) ? required : false;
		this.issueType = issueType;
	}

	public void updateKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateDescription(String description) {
		this.description = (description == null) ? "" : description;
	}

	public void updateFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public void updateRequired(boolean required) {
		this.required = required;
	}
}
