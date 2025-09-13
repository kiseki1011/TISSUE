package com.tissue.api.issue.base.domain.model;

import java.util.Objects;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLRestriction;

import com.tissue.api.common.entity.PrefixedKeyEntity;
import com.tissue.api.global.key.KeyPrefixPolicy;
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
import jakarta.persistence.SequenceGenerator;
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
public class IssueField extends PrefixedKeyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_field_seq_gen")
	@SequenceGenerator(name = "issue_field_seq_gen", sequenceName = "issue_field_seq", allocationSize = 50)
	private Long id;

	@Column(name = "issue_field_key", nullable = false, updatable = false, unique = true)
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

	@Override
	protected void setKey(String key) {
		this.key = key;
	}

	@Override
	protected String keyPrefix() {
		return KeyPrefixPolicy.ISSUE_FIELD;
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
		// TODO: Should I use DomainPreconditions for non-null validation?
		//  예시: DomainPreconditions.requireNotNull, requireNotBlank, ...
		// TODO: Should I use TextNormalizer normaliztion methods I defined, to normalize in the constructor?
		//  예시: TextNormalizer.normalizeLabel, nfc, strip, ...
		this.label = Objects.requireNonNull(label);
		this.description = Objects.requireNonNull(description);
		this.fieldType = Objects.requireNonNull(fieldType);
		this.required = Boolean.TRUE.equals(required);
		this.issueType = Objects.requireNonNull(issueType);
	}

	public String getWorkspaceCode() {
		return issueType.getWorkspaceCode();
	}

	public void updateMetaData(String description, Boolean required) {
		updateDescription(description);
		updateRequired(required);
	}

	// TODO: update 메서드들에도 생성자 처럼 DomainPreconditions, TextNormalizer 사용을 고려해야할까?
	public void rename(String label) {
		this.label = Objects.requireNonNull(label);
	}

	public void updateDescription(String description) {
		this.description = Objects.requireNonNull(description);
	}

	public void updateRequired(Boolean required) {
		this.required = Boolean.TRUE.equals(required);
	}

	public void updateFieldType(FieldType fieldType) {
		this.fieldType = Objects.requireNonNull(fieldType);
	}

	public void softDelete() {
		archive();
	}
}
