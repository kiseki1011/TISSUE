package com.tissue.api.issue.base.domain.model;

import java.util.Objects;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.util.DomainPreconditions;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.vo.Label;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import lombok.ToString;

@Entity
@Getter
@ToString(onlyExplicitlyIncluded = true)
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
	@ToString.Include
	private Long id;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FieldType fieldType;

	@Column(nullable = false)
	private boolean required;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_type_id", nullable = false)
	private IssueType issueType;

	@Builder
	private IssueField(
		Label label,
		String description,
		FieldType fieldType,
		Boolean required,
		IssueType issueType
	) {
		this.label = Objects.requireNonNull(label);
		this.description = DomainPreconditions.nullToEmpty(description);
		this.fieldType = DomainPreconditions.requireNotNull(fieldType, "fieldType");
		this.required = Boolean.TRUE.equals(required);
		this.issueType = DomainPreconditions.requireNotNull(issueType, "issueType");
	}

	public static IssueField create(
		Label label,
		@Nullable String description,
		FieldType fieldType,
		Boolean required,
		IssueType issueType
	) {
		return IssueField.builder()
			.label(label)
			.description(description)
			.fieldType(fieldType)
			.required(required)
			.issueType(issueType)
			.build();
	}

	public String getWorkspaceKey() {
		return issueType.getWorkspaceKey();
	}

	public void rename(Label label) {
		this.label = Objects.requireNonNull(label);
	}

	public void updateDescription(String description) {
		this.description = DomainPreconditions.nullToEmpty(description);
	}

	public void setRequired(Boolean required) {
		this.required = Boolean.TRUE.equals(required);
	}

	public void updateFieldType(FieldType fieldType) {
		this.fieldType = DomainPreconditions.requireNotNull(fieldType, "fieldType");
	}

	public void softDelete() {
		archive();
	}
}
