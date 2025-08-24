package com.tissue.api.issue.base.domain.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.global.key.KeyGenerator;
import com.tissue.api.issue.base.domain.StringListConverter;
import com.tissue.api.issue.base.domain.enums.FieldType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

	@Convert(converter = StringListConverter.class)
	@Column(columnDefinition = "json")
	private List<String> allowedOptions = new ArrayList<>();

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
		IssueType issueType,
		List<String> allowedOptions
	) {
		this.key = key;
		this.label = label;
		this.description = (description != null) ? description : "";
		this.fieldType = fieldType;
		this.required = (required != null) ? required : false;
		this.issueType = issueType;
		this.allowedOptions = allowedOptions;
	}

	public String getWorkspaceCode() {
		return issueType.getWorkspaceCode();
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

	public void updateAllowedOptions(List<String> allowedOptions) {
		this.allowedOptions = allowedOptions;
	}
}
