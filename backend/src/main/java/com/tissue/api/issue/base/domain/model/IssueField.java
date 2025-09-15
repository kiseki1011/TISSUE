package com.tissue.api.issue.base.domain.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.util.DomainPreconditions;
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
		@Index(name = "idx_issue_field_issue_type_label", columnList = "issue_type_id,label")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueField extends BaseEntity {

	// @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_field_seq_gen")
	// @SequenceGenerator(name = "issue_field_seq_gen", sequenceName = "issue_field_seq", allocationSize = 50)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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

	@Builder
	public IssueField(
		String label,
		String description,
		FieldType fieldType,
		Boolean required,
		IssueType issueType
	) {
		this.label = DomainPreconditions.requireNotBlank(label, "label");
		this.description = DomainPreconditions.nullToEmpty(description);
		this.fieldType = DomainPreconditions.requireNotNull(fieldType, "fieldType");
		this.required = Boolean.TRUE.equals(required);
		this.issueType = DomainPreconditions.requireNotNull(issueType, "issueType");
	}

	public String getWorkspaceCode() {
		return issueType.getWorkspaceCode();
	}

	public void updateMetaData(String description, Boolean required) {
		updateDescription(description);
		updateRequired(required);
	}

	public void rename(String label) {
		this.label = DomainPreconditions.requireNotBlank(label, "label");
	}

	public void updateDescription(String description) {
		this.description = DomainPreconditions.nullToEmpty(description);
	}

	public void updateRequired(Boolean required) {
		this.required = Boolean.TRUE.equals(required);
	}

	public void updateFieldType(FieldType fieldType) {
		this.fieldType = DomainPreconditions.requireNotNull(fieldType, "fieldType");
	}

	public void softDelete() {
		archive();
	}
}
