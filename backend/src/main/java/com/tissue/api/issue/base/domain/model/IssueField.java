package com.tissue.api.issue.base.domain.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.global.key.KeyGenerator;
import com.tissue.api.issue.base.domain.enums.FieldType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@SQLRestriction("archived = false")
@Table(
	// uniqueConstraints = {@UniqueConstraint(columnNames = {"issueType_id", "label"})},
	indexes = {
		@Index(name = "idx_issue_field_issue_type_label", columnList = "issue_type_id,label"),
		@Index(name = "idx_issue_field_key", columnList = "key", unique = true)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueField extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false, unique = true)
	private String key;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FieldType fieldType;

	@Column(nullable = false)
	private boolean required;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_type_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private IssueType issueType;

	@PostPersist
	private void assignKey() {
		if (key == null && id != null) {
			key = KeyGenerator.generateIssueFieldKey(id);
		}
	}

	@Builder
	public IssueField(
		String key,
		String label,
		String description,
		FieldType fieldType,
		Boolean required,
		IssueType issueType
	) {
		this.key = key;
		// TODO: use TextPreconditions or a XxxRules class for non-null validation
		this.label = TextNormalizer.normalizeText(label);
		this.description = TextNormalizer.stripToEmpty(description);
		this.fieldType = fieldType;
		this.required = Boolean.TRUE.equals(required);
		this.issueType = issueType;
	}

	public String getWorkspaceCode() {
		return issueType.getWorkspaceCode();
	}

	public void updateMetaData(String description, Boolean required) {
		updateDescription(description);
		updateRequired(required);
	}

	public void rename(String label) {
		// TODO: TextPreconditions.requireNonNull(label);
		this.label = TextNormalizer.normalizeText(label);
	}

	public void updateDescription(String description) {
		this.description = TextNormalizer.stripToEmpty(description);
	}

	public void updateRequired(Boolean required) {
		this.required = Boolean.TRUE.equals(required);
	}

	public void updateFieldType(FieldType fieldType) {
		// TODO: requireNonNull(fieldType);
		this.fieldType = fieldType;
	}

	public void softDelete() {
		archive();
	}
}
