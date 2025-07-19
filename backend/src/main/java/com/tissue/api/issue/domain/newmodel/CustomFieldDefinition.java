package com.tissue.api.issue.domain.newmodel;

import com.tissue.api.common.entity.BaseEntity;

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

// TODO: have i set the UniqueConstraint properly?
//  A CustomFieldDefinition must be unique for each IssueTypeDefinition by label.
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"issueType_id", "label"})
})
@EqualsAndHashCode(of = {"issueType", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomFieldDefinition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String key;

	@Column(nullable = false)
	private String label; // UI label

	private String description;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private FieldType fieldType; // TEXT, NUMBER, DATE, ENUM, etc.

	private boolean required;

	// TODO: Should I use a bi-directional relation with IssueTypeDefinition?
	// TODO: Is @JoinColumn needed or recommended?
	@ManyToOne(fetch = FetchType.LAZY)
	private IssueTypeDefinition issueType;

	@Builder
	public CustomFieldDefinition(String key, String label, String description,
		FieldType fieldType, boolean required, IssueTypeDefinition issueType
	) {
		this.key = key;
		this.label = label;
		this.description = description;
		this.fieldType = fieldType;
		// TODO: Should i set the default value as false if value is null?
		this.required = required;
		this.issueType = issueType;
	}

	public void updateKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public void updateFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public void updateRequired(boolean required) {
		this.required = required;
	}
}
