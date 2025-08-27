package com.tissue.api.issue.base.domain.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"issueType_id", "label"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueField extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
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

	// TODO: Should i make use a bi-directional relation with EnumFieldOption?
	//  IssueField <-> Collection<EnumFieldOption>
	//  If using bi-directional, should I use Set or List?

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
		// TODO: use TextPreconditions for non-null validation
		this.label = TextNormalizer.nfc(label).strip();
		this.description = TextNormalizer.stripToEmpty(description);
		this.fieldType = fieldType;
		this.required = (required != null) ? required : false;
		this.issueType = issueType;
	}

	public String getWorkspaceCode() {
		return issueType.getWorkspaceCode();
	}

	public void updateMetaData(String label, String description, Boolean required) {
		updateLabel(label);
		updateDescription(description);
		updateRequired(required);
	}

	public void updateLabel(String label) {
		// TODO: use TextPreconditions.requireNonNull
		this.label = TextNormalizer.nfc(label).strip();
	}

	public void updateDescription(String description) {
		this.description = TextNormalizer.stripToEmpty(description);
	}

	public void updateRequired(Boolean required) {
		// IssueFieldRules.requireNonNull(required);
		this.required = required;
	}

	public void updateFieldType(FieldType fieldType) {
		// IssueFieldRules.requireNonNull(fieldType)
		this.fieldType = fieldType;
	}
}
