package com.tissue.api.issue.base.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Getter
@ToString(onlyExplicitlyIncluded = true)
@SQLRestriction("archived = false")
@Table(
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

	@Version
	@ToString.Include
	private Long version;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FieldType fieldType;

	@Column(nullable = false)
	private boolean required;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "issue_type_id", nullable = false)
	private IssueType issueType;

	// private String icon;

	@Builder(access = AccessLevel.PRIVATE)
	private IssueField(
		Label label,
		String description,
		FieldType fieldType,
		Boolean required,
		IssueType issueType
	) {
		this.label = label;
		this.description = description;
		this.fieldType = fieldType;
		this.required = required;
		this.issueType = issueType;
	}

	public static IssueField create(
		@NonNull Label label,
		@Nullable String description,
		@NonNull FieldType fieldType,
		@NonNull Boolean required,
		@NonNull IssueType issueType
	) {
		return IssueField.builder()
			.label(label)
			.description(nullToEmpty(description))
			.fieldType(fieldType)
			.required(Boolean.TRUE.equals(required))
			.issueType(issueType)
			.build();
	}

	public String getWorkspaceKey() {
		return issueType.getWorkspaceKey();
	}

	public void rename(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void setRequired(@NonNull Boolean required) {
		this.required = Boolean.TRUE.equals(required);
	}

	// public void updateFieldType(@NonNull FieldType fieldType) {
	// 	this.fieldType = fieldType;
	// }

	public void softDelete() {
		archive();
	}
}
